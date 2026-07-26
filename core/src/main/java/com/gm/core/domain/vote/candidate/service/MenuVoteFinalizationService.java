package com.gm.core.domain.vote.candidate.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.gm.core.domain.group.exception.GroupErrorCode;
import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.repository.GroupRepository;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateErrorCode;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateException;
import com.gm.core.domain.vote.candidate.model.MenuVoteCloseResult;
import com.gm.core.domain.vote.candidate.model.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.MenuVoteResult;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.core.domain.vote.candidate.repository.MenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.FinalMenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;
import com.gm.core.transaction.AfterCommitExecutor;

/**
 * Redis 투표를 닫아 고정된 집계를 얻고 후보별 최종 결과를 DB에 저장한다.
 *
 * <p>세션 행 잠금으로 자동·수동 마감의 중복 처리를 막고, DB 커밋이 끝난 뒤
 * Redis 데이터를 삭제한다. DB 저장이 실패하면 닫힌 Redis 스냅샷을 남겨 재시도한다.</p>
 */
@Service
@RequiredArgsConstructor
public class MenuVoteFinalizationService {

    private final VoteSessionRepository voteSessionRepository;
    private final VoteCandidateRepository voteCandidateRepository;
    private final MenuVoteRepository menuVoteRepository;
    private final GroupRepository groupRepository;
    private final MenuVoteResultPolicy resultPolicy;
    private final AfterCommitExecutor afterCommitExecutor;
    private final FinalMenuVoteRepository finalMenuVoteRepository;

    /**
     * Redis 투표를 원자적으로 닫고 후보별 최종 집계와 판정 결과를 저장한다.
     * DB 저장 실패 시 CLOSED 스냅샷을 유지하며, 저장 완료 후 Redis 데이터를 삭제한다.
     *
     * @param voteSessionId 자동 마감할 투표 세션 식별자
     * @return 저장된 후보별 최종 집계와 판정
     * @throws VoteSessionException 세션이 없거나 메뉴 투표를 마감할 수 없는 상태인 경우
     * @throws VoteCandidateException Redis 투표를 닫거나 스냅샷을 가져올 수 없는 경우
     */
    @Transactional(noRollbackFor = VoteCandidateException.class)
    public List<MenuVoteResult> finalizeVote(UUID voteSessionId) {
        return finalizeVote(voteSessionId, false, null, null);
    }

    /**
     * 응답자가 한 명 이상인 투표를 활성 방장 요청으로 조기 마감한다.
     *
     * @param groupId 경로로 전달된 그룹 식별자
     * @param requesterUserId 마감을 요청한 회원 식별자
     * @param voteSessionId 마감할 투표 세션 식별자
     * @return 저장된 후보별 최종 집계와 판정
     * @throws GroupException 요청자가 활성 방장이 아닌 경우
     * @throws VoteSessionException 그룹과 세션이 일치하지 않거나 마감할 수 없는 상태인 경우
     * @throws VoteCandidateException 응답자가 없거나 Redis 투표를 닫을 수 없는 경우
     */
    @Transactional(noRollbackFor = VoteCandidateException.class)
    public List<MenuVoteResult> finalizeVoteManually(
            UUID groupId,
            UUID requesterUserId,
            UUID voteSessionId
    ) {
        Assert.notNull(groupId, "groupId must not be null");
        Assert.notNull(requesterUserId, "requesterUserId must not be null");
        return finalizeVote(voteSessionId, true, groupId, requesterUserId);
    }

    /**
     * 자동·수동 마감의 공통 흐름을 실행한다.
     *
     * @param voteSessionId 마감할 투표 세션 식별자
     * @param requireAnyResponse 수동 마감에 한 명 이상의 응답을 요구할지 여부
     * @param groupId 수동 마감일 때 검증할 그룹 식별자
     * @param requesterUserId 수동 마감일 때 권한을 확인할 회원 식별자
     * @return 저장됐거나 이미 저장되어 있던 후보별 최종 결과
     */
    private List<MenuVoteResult> finalizeVote(
            UUID voteSessionId,
            boolean requireAnyResponse,
            UUID groupId,
            UUID requesterUserId
    ) {
        Assert.notNull(voteSessionId, "voteSessionId must not be null");
        // 자동 마감과 수동 마감이 겹쳐도 한 요청만 상태를 전환하도록 세션 행을 잠근다.
        VoteSession session = voteSessionRepository.findByIdForUpdate(voteSessionId)
                .orElseThrow(() -> new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
        if (requireAnyResponse) {
            validateManualFinalization(session, groupId, requesterUserId);
        }
        if (session.voteSessionStatus() == VoteSessionStatus.MENU_SELECTION) {
            // DB 저장까지 끝난 재호출은 결과를 다시 쓰지 않고 남은 Redis 정리만 재시도한다.
            List<MenuVoteResult> stored = voteCandidateRepository.findMenuVoteResults(voteSessionId);
            scheduleRedisCleanup(voteSessionId);
            scheduleFinalVoteInitialization(session, remainingCandidateIds(stored));
            return stored;
        }
        if (session.voteSessionStatus() != VoteSessionStatus.MENU_VOTING) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }

        /*
         * 제출 차단과 집계 조회를 하나의 Redis 원자 연산으로 처리한다.
         * 마감 시점의 후보별 갈래·애매·말래 수와 응답자 수가 한 스냅샷으로 고정되므로,
         * 이후 판정과 DB 저장은 서로 다른 시점의 집계가 섞이지 않는다.
         */
        MenuVoteCloseResult closeResult = requireAnyResponse
                ? menuVoteRepository.closeAndGetSnapshotIfAnyResponse(voteSessionId)
                : menuVoteRepository.closeAndGetSnapshot(voteSessionId);
        List<MenuVoteCount> snapshot = snapshotOrThrow(voteSessionId, session, closeResult);

        /*
         * 후보별 응답자 수를 기준으로 최종 판정을 붙인다.
         * 응답 없음은 UNRESOLVED, 갈래가 엄격한 과반이고 말래가 없으면 CONFIRMED,
         * 말래가 절반 이상이면 REJECTED, 나머지는 KEPT로 판정한다.
         */
        List<MenuVoteResult> decisions = snapshot.stream()
                .map(count -> new MenuVoteResult(count, resultPolicy.decide(count)))
                .toList();

        // Redis와 DB의 후보 집합이 모두 일치할 때만 후보별 집계와 판정을 함께 반영한다.
        List<MenuVoteResult> saved = voteCandidateRepository.saveMenuVoteResults(
                voteSessionId,
                decisions
        );

        List<UUID> remainingCandidateIds = remainingCandidateIds(saved);
        VoteSessionStatus nextStatus = remainingCandidateIds.isEmpty()
                ? VoteSessionStatus.MENU_RECOMMENDING
                : VoteSessionStatus.MENU_SELECTION;
        VoteSession finalized = session.changeStatus(nextStatus);
        voteSessionRepository.updateStatus(voteSessionId, finalized.voteSessionStatus())
                .orElseThrow(() -> new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
        scheduleRedisCleanup(voteSessionId);
        scheduleFinalVoteInitialization(session, remainingCandidateIds);
        return saved;
    }

    /** 경로의 그룹과 세션 소속을 확인하고 활성 방장에게만 수동 마감을 허용한다. */
    private void validateManualFinalization(
            VoteSession session,
            UUID groupId,
            UUID requesterUserId
    ) {
        if (!groupId.equals(session.diningGroupId())) {
            throw new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND);
        }
        if (!groupRepository.isActiveOwner(groupId, requesterUserId)) {
            throw new GroupException(GroupErrorCode.NOT_GROUP_OWNER);
        }
    }

    /** Redis 마감 결과를 스냅샷으로 변환하고 유실된 집계는 실패 상태로 종료한다. */
    private List<MenuVoteCount> snapshotOrThrow(
            UUID voteSessionId,
            VoteSession session,
            MenuVoteCloseResult closeResult
    ) {
        return switch (closeResult.status()) {
            case SUCCESS -> closeResult.snapshot();
            case NO_RESPONSE -> throw new VoteCandidateException(
                    VoteCandidateErrorCode.VOTE_CLOSE_NOT_ALLOWED
            );
            case SNAPSHOT_NOT_FOUND -> {
                VoteSession failed = session.changeStatus(VoteSessionStatus.FAILED);
                voteSessionRepository.updateStatus(voteSessionId, failed.voteSessionStatus())
                        .orElseThrow(() -> new VoteSessionException(
                                VoteSessionErrorCode.SESSION_NOT_FOUND
                        ));
                throw new VoteCandidateException(VoteCandidateErrorCode.VOTE_SNAPSHOT_NOT_FOUND);
            }
        };
    }

    /** DB 결과가 실제 커밋된 뒤에만 복구용 Redis 스냅샷을 삭제한다. */
    private void scheduleRedisCleanup(UUID voteSessionId) {
        afterCommitExecutor.execute(() -> menuVoteRepository.delete(voteSessionId));
    }

    /** 정책 판정 뒤 최종 선택 대상으로 남은 후보 식별자만 반환한다. */
    private List<UUID> remainingCandidateIds(List<MenuVoteResult> results) {
        return results.stream()
                .filter(result -> result.result() == VoteCandidateResult.CONFIRMED
                        || result.result() == VoteCandidateResult.KEPT)
                .map(result -> result.count().candidateId())
                .toList();
    }

    /** 커밋 이후 초기화에 실패해도 마감 재호출로 두 후보 최종 투표를 복구할 수 있게 한다. */
    private void scheduleFinalVoteInitialization(VoteSession session, List<UUID> candidateIds) {
        if (candidateIds.size() != 2) {
            return;
        }
        int eligibleVoterCount = groupRepository.findById(session.diningGroupId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND))
                .memberCount();
        afterCommitExecutor.execute(() -> finalMenuVoteRepository.initialize(
                session.id(),
                candidateIds,
                eligibleVoterCount
        ));
    }
}
