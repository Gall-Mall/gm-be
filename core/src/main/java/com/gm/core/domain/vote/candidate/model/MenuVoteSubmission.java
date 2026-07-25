package com.gm.core.domain.vote.candidate.model;

import org.springframework.util.Assert;

/**
 * 사용자 선택 반영 결과와 최신 후보 집계를 담는다.
 *
 * @param choice 요청한 선택
 * @param count 선택 반영 후 후보 집계
 * @param changed 이전 선택과 달라 집계가 변경됐는지 여부
 */
public record MenuVoteSubmission(
        MenuVoteChoice choice,
        MenuVoteCount count,
        boolean changed
) {
    /** 선택과 최신 집계가 모두 있는지 검증한다. */
    public MenuVoteSubmission {
        Assert.notNull(choice, "choice must not be null");
        Assert.notNull(count, "count must not be null");
    }
}
