package com.gm.core.domain.vote.candidate.model;

import org.springframework.util.Assert;

import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;

/**
 * 후보별 최종 집계와 정책 판정 결과이다.
 *
 * @param count 후보별 최종 집계
 * @param result 정책 판정 결과
 */
public record MenuVoteResult(
        MenuVoteCount count,
        VoteCandidateResult result
) {
    /** 최종 집계와 판정이 모두 있는 결과만 생성한다. */
    public MenuVoteResult {
        Assert.notNull(count, "count must not be null");
        Assert.notNull(result, "result must not be null");
    }
}
