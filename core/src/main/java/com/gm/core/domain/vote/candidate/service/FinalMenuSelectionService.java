package com.gm.core.domain.vote.candidate.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.group.exception.GroupErrorCode;
import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.repository.GroupRepository;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateErrorCode;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateException;
import com.gm.core.domain.vote.candidate.model.FinalMenuVoteResult;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.repository.FinalMenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;
import com.gm.core.transaction.AfterCommitExecutor;

/** 1/2/3개 최종 후보를 확정하거나 재추천하는 후속 결정을 수행한다. */
@Service
@RequiredArgsConstructor
public class FinalMenuSelectionService {

    private final VoteSessionRepository voteSessionRepository;
    private final VoteCandidateRepository voteCandidateRepository;
    private final FinalMenuVoteRepository finalMenuVoteRepository;
    private final GroupRepository groupRepository;
    private final GroupService groupService;
    private final AfterCommitExecutor afterCommitExecutor;

    /** 두 후보일 때 활성 그룹원의 Redis 최종 투표를 제출한다. */
    @Transactional
    public FinalMenuVoteResult submitFinalVote(
            UUID groupId,
            UUID userId,
            UUID voteSessionId,
            UUID candidateId
    ) {
        groupService.findGroupDetail(groupId, userId);
        // 방장 선택 경로와 동일하게 세션 행을 먼저 잠가 후보 행과의 잠금 순서를 고정한다.
        VoteSession session = lockedSession(groupId, voteSessionId);
        if (session.voteSessionStatus() != VoteSessionStatus.MENU_SELECTION) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }
        List<UUID> candidates = voteCandidateRepository
                .findRemainingCandidateIdsForUpdate(voteSessionId);
        if (candidates.size() != 2 || !candidates.contains(candidateId)) {
            throw new VoteCandidateException(VoteCandidateErrorCode.FINAL_VOTE_NOT_ALLOWED);
        }

        FinalMenuVoteResult result = finalMenuVoteRepository.submit(
                voteSessionId,
                userId,
                candidateId
        );
        if (result.status() == FinalMenuVoteResult.Status.SELECTED) {
            selectLocked(session, result.selectedCandidateId());
        }
        return result;
    }

    /** 1개·3개 후보 또는 두 후보 최종 투표 동점일 때 활성 방장이 최종 메뉴를 선택한다. */
    @Transactional
    public VoteCandidate selectByOwner(
            UUID groupId,
            UUID ownerUserId,
            UUID voteSessionId,
            UUID candidateId
    ) {
        requireOwner(groupId, ownerUserId);
        VoteSession session = lockedSession(groupId, voteSessionId);
        if (session.voteSessionStatus() != VoteSessionStatus.MENU_SELECTION) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }
        List<UUID> candidates = voteCandidateRepository
                .findRemainingCandidateIdsForUpdate(voteSessionId);
        boolean ownerChoiceAllowed = candidates.size() == 1
                || candidates.size() >= 3
                || (candidates.size() == 2
                && finalMenuVoteRepository.isTiedCandidate(voteSessionId, candidateId));
        if (!ownerChoiceAllowed || !candidates.contains(candidateId)) {
            throw new VoteCandidateException(VoteCandidateErrorCode.FINAL_VOTE_TIE_REQUIRED);
        }
        return selectLocked(session, candidateId);
    }

    /** 후보가 하나만 남았을 때 활성 방장이 확정 대신 재추천을 선택한다. */
    @Transactional
    public void reRecommendSingleCandidate(UUID groupId, UUID ownerUserId, UUID voteSessionId) {
        requireOwner(groupId, ownerUserId);
        VoteSession session = lockedSession(groupId, voteSessionId);
        List<UUID> candidates = voteCandidateRepository
                .findRemainingCandidateIdsForUpdate(voteSessionId);
        if (session.voteSessionStatus() != VoteSessionStatus.MENU_SELECTION
                || candidates.size() != 1) {
            throw new VoteCandidateException(
                    VoteCandidateErrorCode.FINAL_MENU_SELECTION_NOT_ALLOWED);
        }
        VoteSession next = session.changeStatus(VoteSessionStatus.MENU_RECOMMENDING);
        voteSessionRepository.updateStatus(voteSessionId, next.voteSessionStatus())
                .orElseThrow(() -> new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
    }

    /**
     * deadline으로 닫힌 두 후보 최종투표의 단독 1위를 기존 DB 잠금 구조로 확정한다.
     * 이미 확정된 세션의 재호출은 Redis 정리만 다시 예약해 멱등 처리한다.
     *
     * @param voteSessionId 자동 마감된 투표 세션 식별자
     * @param candidateId Redis 집계의 단독 1위 후보 식별자
     */
    @Transactional
    public void selectExpiredWinner(UUID voteSessionId, UUID candidateId) {
        VoteSession session = voteSessionRepository.findByIdForUpdate(voteSessionId)
                .orElseThrow(() -> new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
        if (session.voteSessionStatus() == VoteSessionStatus.RESTAURANT_SEARCHING) {
            // DB 확정 뒤 Redis 정리만 실패한 재호출은 후보를 다시 선택하지 않는다.
            afterCommitExecutor.execute(() -> finalMenuVoteRepository.delete(voteSessionId));
            return;
        }
        if (session.voteSessionStatus() != VoteSessionStatus.MENU_SELECTION) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }
        List<UUID> candidates = voteCandidateRepository
                .findRemainingCandidateIdsForUpdate(voteSessionId);
        if (candidates.size() != 2 || !candidates.contains(candidateId)) {
            throw new VoteCandidateException(VoteCandidateErrorCode.FINAL_VOTE_NOT_ALLOWED);
        }
        selectLocked(session, candidateId);
    }


    /** 잠긴 세션에서 후보 하나를 최종 선택하고 식당 검색 단계로 전환한다. */
    private VoteCandidate selectLocked(VoteSession session, UUID candidateId) {
        VoteCandidate selected = voteCandidateRepository.selectFinalCandidate(session.id(), candidateId);
        VoteSession next = session.changeStatus(VoteSessionStatus.RESTAURANT_SEARCHING);
        voteSessionRepository.updateStatus(session.id(), next.voteSessionStatus())
                .orElseThrow(() -> new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
        // DB 후보 선택과 상태 변경이 커밋된 뒤에만 Redis 임시 투표를 지운다.
        afterCommitExecutor.execute(() -> finalMenuVoteRepository.delete(session.id()));
        return selected;
    }


    /** 후보 잠금보다 세션 잠금을 먼저 얻고 경로의 그룹 소속까지 확인한다. */
    private VoteSession lockedSession(UUID groupId, UUID voteSessionId) {
        VoteSession session = voteSessionRepository.findByIdForUpdate(voteSessionId)
                .orElseThrow(() -> new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
        requireGroup(session, groupId);
        return session;
    }

    /** 다른 그룹의 세션 식별자를 사용한 요청을 찾을 수 없는 세션으로 처리한다. */
    private void requireGroup(VoteSession session, UUID groupId) {
        if (!groupId.equals(session.diningGroupId())) {
            throw new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND);
        }
    }

    /** 현재 활성 방장만 직접 선택과 재추천을 수행하도록 제한한다. */
    private void requireOwner(UUID groupId, UUID userId) {
        if (!groupRepository.isActiveOwner(groupId, userId)) {
            throw new GroupException(GroupErrorCode.NOT_GROUP_OWNER);
        }
    }
}
