package com.gm.core.domain.vote.candidate.model;

import org.springframework.util.Assert;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Redis에 메뉴 투표를 열 때 필요한 불변 세션 메타데이터다.
 * 시작 시각과 마감 시각은 이 정보를 받은 저장소가 신뢰 가능한 서버 시간을 기준으로 계산한다.
 *
 * @param voteSessionId 투표 세션 ID
 * @param candidateIds 세션에 속한 메뉴 후보 ID 목록
 * @param votingDuration 투표 가능 시간
 */
public record MenuVoteSession(
        UUID voteSessionId,
        List<UUID> candidateIds,
        Duration votingDuration
) {
    /** 세션 식별자, 후보 소속, 투표 가능 시간의 불변조건을 검증한다. */
    public MenuVoteSession {
        Assert.notNull(voteSessionId, "voteSessionId must not be null");
        Assert.notEmpty(candidateIds, "candidateIds must not be empty");
        Assert.noNullElements(candidateIds, "candidateIds must not contain null");
        Assert.isTrue(
                new HashSet<>(candidateIds).size() == candidateIds.size(),
                "candidateIds must not contain duplicates"
        );
        Assert.notNull(votingDuration, "votingDuration must not be null");
        Assert.isTrue(
                !votingDuration.isZero() && !votingDuration.isNegative(),
                "votingDuration must be positive"
        );
        candidateIds = List.copyOf(candidateIds);
    }
}
