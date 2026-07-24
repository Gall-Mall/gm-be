package com.gm.core.domain.vote.candidate.repository;

import java.util.List;
import java.util.UUID;

import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
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
    List<VoteCandidate> saveAll(List<VoteCandidate> candidates);

    /**
     * 세션의 메뉴 후보를 노출 순서대로 조회한다.
     *
     * @param voteSessionId 투표 세션 식별자
     * @return 메뉴 정보와 집계를 포함한 후보 목록
     */
    List<MenuVoteCandidate> findAllByVoteSessionId(UUID voteSessionId);
}
