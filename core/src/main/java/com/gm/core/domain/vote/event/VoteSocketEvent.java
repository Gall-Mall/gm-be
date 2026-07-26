package com.gm.core.domain.vote.event;

import java.time.Instant;
import java.util.UUID;

import org.springframework.util.Assert;

/** 투표 세션 topic으로 전달할 공통 Socket 이벤트 payload다. */
public record VoteSocketEvent(
        VoteEventType eventType,
        UUID voteSessionId,
        Instant occurredAt,
        Object data
) {
    /** Socket 이벤트의 필수 식별 정보와 발생 시각을 확인한다. */
    public VoteSocketEvent {
        Assert.notNull(eventType, "eventType must not be null");
        Assert.notNull(voteSessionId, "voteSessionId must not be null");
        Assert.notNull(occurredAt, "occurredAt must not be null");
    }

    /** 현재 시각으로 투표 Socket 이벤트를 만든다. */
    public static VoteSocketEvent now(VoteEventType eventType, UUID voteSessionId, Object data) {
        return new VoteSocketEvent(eventType, voteSessionId, Instant.now(), data);
    }
}
