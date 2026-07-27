package com.gm.core.domain.group.service;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.group.exception.GroupErrorCode;
import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.GroupDetail;
import com.gm.core.domain.group.model.GroupMember;
import com.gm.core.domain.group.model.GroupUpdate;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.core.domain.group.repository.GroupRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    /**
     * 그룹을 생성하고 요청 회원을 그룹장으로 등록한다.
     *
     * @param newGroup 그룹 생성 명세
     * @return 생성된 그룹
     */
    @Transactional
    public Group create(NewGroup newGroup) {
        log.info("그룹 생성: ownerUserId: {}, name: {}", newGroup.ownerUserId(), newGroup.name());
        return groupRepository.create(newGroup);
    }

    /**
     * 요청 회원이 활성 멤버로 참여 중인 그룹 목록을 페이지 단위 조회한다.
     *
     * @param userId 요청 회원 식별자
     * @param pageable 페이지 번호·크기
     * @return 참여 중인 그룹 페이지
     */
    @Transactional(readOnly = true)
    public Page<Group> findMyGroups(UUID userId, Pageable pageable) {
        log.info("내 그룹 목록 조회: userId: {}, page: {}, size: {}", userId, pageable.getPageNumber(), pageable.getPageSize());
        return groupRepository.findAllByMemberUserId(userId, pageable);
    }

    /**
     * 그룹 상세 정보와 요청 회원의 역할을 조회한다.
     *
     * @param groupId 조회할 그룹 식별자
     * @param userId 요청 회원 식별자
     * @return 그룹 정보와 요청 회원의 역할
     * @throws GroupException groupId에 해당하는 그룹이 없어 {@code GROUP-001} 오류가 발생하는 경우
     * @throws GroupException 요청 회원이 활성 멤버가 아니어서 {@code GROUP-002} 오류가 발생하는 경우
     */
    @Transactional(readOnly = true)
    public GroupDetail findGroupDetail(UUID groupId, UUID userId) {
        log.info("그룹 상세 조회: groupId: {}, userId: {}", groupId, userId);
        return groupRepository.findDetailByIdAndMemberUserId(groupId, userId)
                .orElseGet(() -> {
                    if (!groupRepository.existsById(groupId)) {
                        throw new GroupException(GroupErrorCode.GROUP_NOT_FOUND);
                    }
                    throw new GroupException(GroupErrorCode.NOT_GROUP_MEMBER);
                });
    }

    /**
     * 그룹 정원을 확인한 뒤 요청 회원을 활성 멤버로 등록한다. 정원 확인과 등록은 하나의
     * 원자적 흐름으로 처리된다. 자발적으로 탈퇴(LEFT)했던 회원은 재가입할 수 있지만,
     * 강퇴(KICKED)된 회원은 재가입할 수 없다.
     *
     * @param groupId 가입할 그룹 식별자
     * @param userId 가입할 회원 식별자
     * @return 등록(또는 재활성화)된 멤버십 (요청 회원이 이미 활성 멤버였다면 빈 값)
     * @throws GroupException groupId에 해당하는 그룹이 없어 {@code GROUP-001} 오류가 발생하는 경우
     * @throws GroupException 그룹 정원이 가득 차 {@code GROUP-004} 오류가 발생하는 경우
     * @throws GroupException 요청 회원이 강퇴 이력이 있어 {@code GROUP-007} 오류가 발생하는 경우
     */
    @Transactional
    public Optional<GroupMember> addMember(UUID groupId, UUID userId) {
        log.info("그룹 멤버 추가: groupId: {}, userId: {}", groupId, userId);
        return groupRepository.addActiveMember(groupId, userId);
    }

    /**
     * 요청 회원이 groupId 그룹의 활성 방장 멤버인지 확인한 뒤 그룹 설정 전체를 groupUpdate
     * 내용으로 교체한다. 방장 여부는 {@code group_member}의 역할(role)·상태(status)를 기준으로
     * 판단한다(그룹의 {@code owner_user_id}는 참조하지 않는다).
     *
     * @param groupId 수정할 그룹 식별자
     * @param requesterUserId 요청 회원 식별자
     * @param groupUpdate 교체할 그룹 설정
     * @return 수정된 그룹
     * @throws GroupException groupId에 해당하는 그룹이 없어 {@code GROUP-001} 오류가 발생하는 경우
     * @throws GroupException 요청 회원이 활성 방장 멤버가 아니어서 {@code GROUP-006} 오류가 발생하는 경우
     * @throws GroupException 정원을 현재 활성 멤버 수보다 작게 변경하려 해 {@code GROUP-008} 오류가
     *         발생하는 경우
     */
    @Transactional
    public Group update(UUID groupId, UUID requesterUserId, GroupUpdate groupUpdate) {
        log.info("그룹 정보 수정: groupId: {}, requesterUserId: {}", groupId, requesterUserId);
        if (!groupRepository.existsById(groupId)) {
            throw new GroupException(GroupErrorCode.GROUP_NOT_FOUND);
        }
        if (!groupRepository.isActiveOwner(groupId, requesterUserId)) {
            throw new GroupException(GroupErrorCode.NOT_GROUP_OWNER);
        }

        return groupRepository.update(groupId, groupUpdate);
    }

    /**
     * 그룹을 삭제한다. 그룹장만 수행할 수 있다.
     *
     * <p>그룹에 딸린 멤버십·투표 세션·후보·추천 식당은 함께 삭제된다. 되돌릴 수 없다.</p>
     *
     * @param groupId 삭제할 그룹 식별자
     * @param requesterUserId 삭제를 요청한 회원 식별자
     * @throws GroupException groupId에 해당하는 그룹이 없어 {@code GROUP-001} 오류가 발생하는 경우
     * @throws GroupException 요청 회원이 활성 방장 멤버가 아니어서 {@code GROUP-006} 오류가
     *         발생하는 경우
     */
    @Transactional
    public void delete(UUID groupId, UUID requesterUserId) {
        log.info("그룹 삭제: groupId: {}, requesterUserId: {}", groupId, requesterUserId);
        if (!groupRepository.existsById(groupId)) {
            throw new GroupException(GroupErrorCode.GROUP_NOT_FOUND);
        }
        if (!groupRepository.isActiveOwner(groupId, requesterUserId)) {
            throw new GroupException(GroupErrorCode.NOT_GROUP_OWNER);
        }

        groupRepository.deleteById(groupId);
    }
}
