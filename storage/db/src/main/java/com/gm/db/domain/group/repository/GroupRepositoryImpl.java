package com.gm.db.domain.group.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.gm.core.domain.group.exception.GroupErrorCode;
import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.GroupDetail;
import com.gm.core.domain.group.model.GroupMember;
import com.gm.core.domain.group.model.GroupMemberProfile;
import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.model.GroupMemberStatus;
import com.gm.core.domain.group.model.GroupUpdate;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.core.domain.group.repository.GroupRepository;
import com.gm.db.domain.group.entity.DiningGroupEntity;
import com.gm.db.domain.group.entity.GroupMemberEntity;
import com.gm.db.domain.group.mapper.GroupMapper;
import com.gm.db.domain.group.mapper.GroupMemberMapper;

@Repository
@RequiredArgsConstructor
public class GroupRepositoryImpl implements GroupRepository {

    private final GroupJpaRepository groupJpaRepository;
    private final GroupMemberJpaRepository groupMemberJpaRepository;
    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;

    @Override
    public Group create(NewGroup newGroup) {
        DiningGroupEntity group = groupJpaRepository.save(groupMapper.toEntity(newGroup));
        groupMemberJpaRepository.save(GroupMemberEntity.ofOwner(group.getId(), newGroup.ownerUserId()));
        return groupMapper.toDomainModel(group, 1);
    }

    @Override
    public Page<Group> findAllByMemberUserId(UUID userId, Pageable pageable) {
        Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return groupJpaRepository
                .findAllWithMemberCountByMemberUserIdAndStatus(userId, GroupMemberStatus.ACTIVE, pageOnly)
                .map(groupMapper::toDomainModel);
    }

    @Override
    public Optional<GroupDetail> findDetailByIdAndMemberUserId(UUID groupId, UUID userId) {
        return groupJpaRepository
                .findDetailByIdAndMemberUserIdAndStatus(groupId, userId, GroupMemberStatus.ACTIVE)
                .map(projection -> new GroupDetail(
                        groupMapper.toDomainModel(projection),
                        projection.currentUserRole()
                ));
    }

    @Override
    public boolean existsById(UUID groupId) {
        return groupJpaRepository.existsById(groupId);
    }

    @Override
    public Optional<Group> findById(UUID groupId) {
        return groupJpaRepository
                .findByIdWithMemberCount(groupId, GroupMemberStatus.ACTIVE)
                .map(groupMapper::toDomainModel);
    }

    /**
     * 그룹 행을 비관적 쓰기 잠금으로 조회한 뒤, 같은 트랜잭션 안에서 기존 멤버십 행의 상태를
     * 확인하고 정원 확인·멤버 등록(또는 재활성화)까지 하나의 원자적 흐름으로 처리한다. 잠금
     * 덕분에 동일 그룹에 대한 동시 가입 요청은 이 메서드 호출 단위로 직렬화되어 정원 초과가
     * 발생하지 않는다.
     *
     * <p>유니크 제약({@code UK_group_member})이 그룹당 회원 한 명에 행 하나만 허용하고 상태를
     * 구분하지 않으므로, 기존 행이 있으면 그 상태에 따라 분기한다:</p>
     * <ul>
     *     <li>{@code ACTIVE}: 이미 가입한 상태이므로 {@link Optional#empty()}를 반환해 호출자
     *         (Invite 도메인)가 "이미 가입함"의 의미를 자신의 맥락에 맞게 해석하도록 한다.</li>
     *     <li>{@code KICKED}: 정원과 무관하게 재가입을 거부한다.</li>
     *     <li>{@code LEFT}: 정원을 확인한 뒤 같은 행을 재활성화한다(새 행을 삽입하지 않는다).</li>
     * </ul>
     * <p>기존 행이 없으면 정원을 확인한 뒤 새 행을 삽입한다. 사전 조회와 삽입 사이의 경쟁
     * 상태는 {@link DataIntegrityViolationException} 캐치로 한 번 더 방어한다.</p>
     */
    @Override
    public Optional<GroupMember> addActiveMember(UUID groupId, UUID userId) {
        DiningGroupEntity group = groupJpaRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        Optional<GroupMemberEntity> existingMember =
                groupMemberJpaRepository.findByDiningGroupIdAndUserId(groupId, userId);

        if (existingMember.isPresent()) {
            GroupMemberEntity member = existingMember.get();
            return switch (member.getStatus()) {
                case ACTIVE -> Optional.empty();
                case KICKED -> throw new GroupException(GroupErrorCode.MEMBER_KICKED);
                case LEFT -> {
                    assertHasCapacity(groupId, group.getMaxMemberCount());
                    member.rejoin();
                    yield Optional.of(groupMemberMapper.toDomainModel(member));
                }
            };
        }

        assertHasCapacity(groupId, group.getMaxMemberCount());
        try {
            GroupMemberEntity saved = groupMemberJpaRepository.save(GroupMemberEntity.ofMember(groupId, userId));
            return Optional.of(groupMemberMapper.toDomainModel(saved));
        } catch (DataIntegrityViolationException exception) {
            // 사전 조회와 삽입 사이의 경쟁 상태에 대한 방어. 유니크 제약이 대신 막아준 경우다.
            return Optional.empty();
        }
    }

    @Override
    public boolean isActiveOwner(UUID groupId, UUID userId) {
        return groupMemberJpaRepository.existsByDiningGroupIdAndUserIdAndRoleAndStatus(
                groupId, userId, GroupMemberRole.OWNER, GroupMemberStatus.ACTIVE);
    }

    /** {@inheritDoc} */
    @Override
    public List<GroupMemberProfile> findActiveMembers(UUID groupId) {
        return groupMemberJpaRepository
                .findProfilesByGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE)
                .stream()
                .map(profile -> new GroupMemberProfile(
                        profile.getGroupMemberId(),
                        profile.getUserId(),
                        profile.getName(),
                        profile.getEmail(),
                        profile.getRole(),
                        profile.getStatus()
                ))
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<String> findActiveOwnerName(UUID groupId) {
        return findActiveMembers(groupId).stream()
                .filter(member -> member.role() == GroupMemberRole.OWNER)
                .map(GroupMemberProfile::name)
                .findFirst();
    }

    /** {@inheritDoc} */
    @Override
    public void transferOwnership(UUID groupId, UUID currentOwnerUserId, UUID nextOwnerUserId) {
        groupJpaRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        GroupMemberEntity currentOwner = groupMemberJpaRepository
                .findByDiningGroupIdAndUserId(groupId, currentOwnerUserId)
                .filter(member -> member.getStatus() == GroupMemberStatus.ACTIVE
                        && member.getRole() == GroupMemberRole.OWNER)
                .orElseThrow(() -> new GroupException(GroupErrorCode.NOT_GROUP_OWNER));
        GroupMemberEntity nextOwner = groupMemberJpaRepository
                .findByDiningGroupIdAndUserId(groupId, nextOwnerUserId)
                .filter(member -> member.getStatus() == GroupMemberStatus.ACTIVE)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (currentOwner.getUserId().equals(nextOwner.getUserId())) {
            return;
        }
        currentOwner.changeRole(GroupMemberRole.MEMBER);
        nextOwner.changeRole(GroupMemberRole.OWNER);
    }

    /** {@inheritDoc} */
    @Override
    public void kickMember(UUID groupId, UUID userId) {
        groupJpaRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        GroupMemberEntity member = groupMemberJpaRepository
                .findByDiningGroupIdAndUserId(groupId, userId)
                .filter(candidate -> candidate.getStatus() == GroupMemberStatus.ACTIVE)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_MEMBER_NOT_FOUND));
        if (member.getRole() == GroupMemberRole.OWNER) {
            throw new GroupException(GroupErrorCode.OWNER_CANNOT_BE_KICKED);
        }
        member.kick();
    }

    /**
     * 그룹 행을 비관적 쓰기 잠금으로 조회해, 동시에 진행 중인 가입 처리({@link #addActiveMember})와
     * 같은 락 정책으로 직렬화한다. 현재 활성 멤버 수보다 작은 정원으로는 변경할 수 없다.
     * 변경 후 {@code updatedAt}은 flush 시점에만 채워지므로, 응답에 최신 값을 반영하기 위해
     * dirty checking에 맡기지 않고 {@code saveAndFlush}로 즉시 flush한다.
     */
    @Override
    public Group update(UUID groupId, GroupUpdate groupUpdate) {
        DiningGroupEntity group = groupJpaRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        long activeMemberCount = groupMemberJpaRepository
                .countByDiningGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE);
        if (activeMemberCount > groupUpdate.maxMemberCount()) {
            throw new GroupException(GroupErrorCode.GROUP_CAPACITY_BELOW_ACTIVE_MEMBERS);
        }

        group.update(
                groupUpdate.name(),
                groupUpdate.locationAddress(),
                groupUpdate.latitude(),
                groupUpdate.longitude(),
                groupUpdate.searchRadiusM(),
                groupUpdate.recommendationTime(),
                groupUpdate.maxMemberCount()
        );

        DiningGroupEntity saved = groupJpaRepository.saveAndFlush(group);
        return groupMapper.toDomainModel(saved, (int) activeMemberCount);
    }

    /**
     * 그룹 행을 삭제한다. 멤버십·투표 세션 이하 데이터는 스키마의 {@code ON DELETE CASCADE}가
     * 함께 정리한다.
     *
     * <p>삭제 대상 조회에 쓰기 잠금을 건다. 잠그지 않으면 동시에 들어온 가입 처리
     * ({@link #addActiveMember})가 이미 지워진 그룹에 멤버를 붙이려다 FK 오류로 실패한다.</p>
     */
    @Override
    public void deleteById(UUID groupId) {
        DiningGroupEntity group = groupJpaRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        groupJpaRepository.delete(group);
    }

    private void assertHasCapacity(UUID groupId, int maxMemberCount) {
        long activeMemberCount = groupMemberJpaRepository
                .countByDiningGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE);
        if (activeMemberCount >= maxMemberCount) {
            throw new GroupException(GroupErrorCode.GROUP_FULL);
        }
    }
}
