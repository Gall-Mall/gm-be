package com.gm.core.domain.vote.candidate.model;

import java.util.UUID;

import org.springframework.util.Assert;

/**
 * 메뉴 후보 한 건의 현재 투표 집계다.
 *
 * @param candidateId 메뉴 후보 ID
 * @param goCount 갈래 선택 수
 * @param maybeCount 애매 선택 수
 * @param noCount 말래 선택 수
 * @param respondentCount 이 후보에 응답한 사용자 수
 */
public record MenuVoteCount(
        UUID candidateId,
        int goCount,
        int maybeCount,
        int noCount,
        int respondentCount
) {
    /** 후보 ID와 집계 값의 정합성을 검증한다. */
    public MenuVoteCount {
        Assert.notNull(candidateId, "candidateId must not be null");
        Assert.isTrue(goCount >= 0, "goCount must not be negative");
        Assert.isTrue(maybeCount >= 0, "maybeCount must not be negative");
        Assert.isTrue(noCount >= 0, "noCount must not be negative");
        Assert.isTrue(respondentCount >= 0, "respondentCount must not be negative");
        Assert.isTrue(
                goCount + maybeCount + noCount == respondentCount,
                "vote counts must equal respondentCount"
        );
    }
}
