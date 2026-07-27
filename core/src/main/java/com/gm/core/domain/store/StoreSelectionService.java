package com.gm.core.domain.store;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.group.exception.GroupErrorCode;
import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.model.GroupDetail;
import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.store.model.Store;
import com.gm.core.domain.store.repository.StoreRepository;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.service.VoteSessionService;

/** 투표 결과에 따른 식당 후보 조회와 최종 식당 선택 유스케이스를 수행한다. */
@Service
@RequiredArgsConstructor
public class StoreSelectionService {

    private final StoreRepository storeRepository;
    private final VoteSessionService voteSessionService;
    private final GroupService groupService;

    /**
     * 그룹 멤버에게 최종 선택 단계의 식당 후보를 반환한다.
     *
     * @param groupId 세션이 속한 그룹 식별자
     * @param userId 조회를 요청한 회원 식별자
     * @param voteSessionId 조회할 투표 세션 식별자
     * @return 거리순으로 정렬된 식당 후보 목록
     * @throws VoteSessionException 세션이 없거나 식당 선택 단계가 아닌 경우
     */
    @Transactional(readOnly = true)
    public List<Store> findResults(UUID groupId, UUID userId, UUID voteSessionId) {
        VoteSession session = voteSessionService.findVoteSession(voteSessionId);
        validateGroup(session, groupId);
        groupService.findGroupDetail(groupId, userId);
        if (session.voteSessionStatus() != VoteSessionStatus.RESTAURANT_SELECTION
                && session.voteSessionStatus() != VoteSessionStatus.COMPLETED) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }
        return storeRepository.findAllByVoteSessionId(voteSessionId);
    }

    /**
     * 그룹장이 식당 후보 하나를 최종 선택하고 투표 세션을 완료한다.
     *
     * @param groupId 세션이 속한 그룹 식별자
     * @param userId 선택을 요청한 회원 식별자
     * @param voteSessionId 완료할 투표 세션 식별자
     * @param externalPlaceId 최종 선택할 외부 장소 식별자
     * @param completedAt 세션 완료 시각
     * @return 최종 선택된 식당
     * @throws GroupException 요청 회원이 그룹장이 아닌 경우
     * @throws VoteSessionException 세션이 없거나 식당 선택 단계가 아닌 경우
     */
    @Transactional
    public Store selectFinalRestaurant(
            UUID groupId,
            UUID userId,
            UUID voteSessionId,
            String externalPlaceId,
            LocalDateTime completedAt
    ) {
        VoteSession session = voteSessionService.findVoteSessionForUpdate(voteSessionId);
        validateGroup(session, groupId);
        GroupDetail group = groupService.findGroupDetail(groupId, userId);
        if (group.currentUserRole() != GroupMemberRole.OWNER) {
            throw new GroupException(GroupErrorCode.NOT_GROUP_OWNER);
        }
        if (session.voteSessionStatus() != VoteSessionStatus.RESTAURANT_SELECTION) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }

        Store selected = storeRepository.selectAsFinalRestaurant(voteSessionId, externalPlaceId);
        voteSessionService.completeVoteSession(voteSessionId, completedAt);
        return selected;
    }

    /**
     * 요청 경로의 그룹과 투표 세션의 소속 그룹이 일치하는지 확인한다.
     *
     * @param session 확인할 투표 세션
     * @param groupId 요청 경로의 그룹 식별자
     * @throws VoteSessionException 두 그룹이 일치하지 않는 경우
     */
    private void validateGroup(VoteSession session, UUID groupId) {
        if (!session.diningGroupId().equals(groupId)) {
            throw new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND);
        }
    }
}
