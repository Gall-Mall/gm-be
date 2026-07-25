package com.gm.api.controller.vote.candidate.dto.response;

import java.util.UUID;

import com.gm.core.domain.vote.candidate.model.MenuVoteResult;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;

/**
 * 1차 메뉴 투표 마감 시 고정된 후보별 집계와 판정이다.
 *
 * @param candidateId 메뉴 후보 식별자
 * @param goCount 갈래 응답 수
 * @param maybeCount 애매 응답 수
 * @param noCount 말래 응답 수
 * @param respondentCount 이 후보에 응답한 회원 수
 * @param result 최종 판정
 */
public record MenuVoteResultResponse(
        UUID candidateId,
        int goCount,
        int maybeCount,
        int noCount,
        int respondentCount,
        VoteCandidateResult result
) {
    /**
     * 저장된 후보별 최종 결과를 API 응답으로 변환한다.
     *
     * @param voteResult 후보별 최종 집계와 판정
     * @return 메뉴 투표 마감 응답
     */
    public static MenuVoteResultResponse from(MenuVoteResult voteResult) {
        return new MenuVoteResultResponse(
                voteResult.count().candidateId(),
                voteResult.count().goCount(),
                voteResult.count().maybeCount(),
                voteResult.count().noCount(),
                voteResult.count().respondentCount(),
                voteResult.result()
        );
    }
}
