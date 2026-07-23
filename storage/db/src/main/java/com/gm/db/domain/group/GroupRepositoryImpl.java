package com.gm.db.domain.group;

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
import com.gm.core.domain.group.model.GroupMemberStatus;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.core.domain.group.repository.GroupRepository;

@Repository
@RequiredArgsConstructor
public class GroupRepositoryImpl implements GroupRepository {

    private final GroupJpaRepository groupJpaRepository;
    private final GroupMemberJpaRepository groupMemberJpaRepository;
    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;

    @Override
    public Group create(NewGroup newGroup) {
        GroupEntity group = groupJpaRepository.save(groupMapper.toEntity(newGroup));
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
        GroupEntity group = groupJpaRepository.findByIdForUpdate(groupId)
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

    private void assertHasCapacity(UUID groupId, int maxMemberCount) {
        long activeMemberCount = groupMemberJpaRepository
                .countByDiningGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE);
        if (activeMemberCount >= maxMemberCount) {
            throw new GroupException(GroupErrorCode.GROUP_FULL);
        }
    }
}
