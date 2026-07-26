package com.gm.core.event.payload;

import java.util.UUID;

import com.gm.core.event.DomainEvent;

/** 전원 투표 완료 — 방장 카카오톡 알림 트리거. */
public record VoteAllCompleted(UUID groupId, UUID voteSessionId, UUID ownerUserId)
        implements DomainEvent {

    public static final String TYPE = "vote.all.completed";

    @Override
    public String eventType() {
        return TYPE;
    }
}
