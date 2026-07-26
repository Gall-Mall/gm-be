package com.gm.core.domain.vote.candidate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.MenuVoteResult;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;

/**
 * 메뉴 투표 후보 저장과 조회 기능을 제공한다.
 */
public interface VoteCandidateRepository {

    /**
     * 추천된 메뉴 후보를 저장한다.
     *
     * @param candidates 저장할 후보 목록
     * @return 저장된 후보 목록
     */
    List<VoteCandidate> saveNewCandidates(List<VoteCandidate> candidates);

    /**
     * 세션의 메뉴 후보를 노출 순서대로 조회한다.
     *
     * @param voteSessionId 투표 세션 식별자
     * @return 메뉴 정보와 집계를 포함한 후보 목록
     */
    List<MenuVoteCandidate> findAllByVoteSessionId(UUID voteSessionId);

    /** 세션에서 이미 확정된 최종 후보를 조회한다. */
    Optional<VoteCandidate> findSelectedCandidate(UUID voteSessionId);

    /**
     * 세션 후보의 최종 집계와 판정 결과를 같은 저장 흐름으로 반영한다.
     *
     * @param voteSessionId 투표 세션 식별자
     * @param results Redis 고정 스냅샷을 판정한 결과
     * @return 저장된 최종 결과
     */
    List<MenuVoteResult> saveMenuVoteResults(UUID voteSessionId, List<MenuVoteResult> results);

    /**
     * 이미 확정된 세션의 최종 결과를 노출 순서대로 조회한다.
     *
     * @param voteSessionId 투표 세션 식별자
     * @return 저장된 최종 결과
     */
    List<MenuVoteResult> findMenuVoteResults(UUID voteSessionId);

    /** 최종 후보(CONFIRMED, KEPT)를 행 잠금으로 노출 순서대로 조회한다. */
    List<UUID> findRemainingCandidateIdsForUpdate(UUID voteSessionId);

    /** 세션에서 정확히 한 후보만 selected=true로 만들고 선택된 후보를 반환한다. */
    VoteCandidate selectFinalCandidate(UUID voteSessionId, UUID candidateId);

}
