package com.gm.core.event.payload;

import java.util.UUID;

import com.gm.core.event.DomainEvent;

/** 식당 검색 완료 — 방장 화면 push 트리거. */
public record StoreSearchCompleted(UUID voteSessionId) implements DomainEvent {

    public static final String TYPE = "group.store.search.completed";

    @Override
    public String eventType() {
        return TYPE;
    }
}
