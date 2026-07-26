package com.gm.core.event.payload;

import java.util.UUID;

import com.gm.core.domain.store.model.Coordinate;
import com.gm.core.event.DomainEvent;

/** 식당 검색 요청 — 지도 API 호출 트리거. */
public record StoreSearchRequested(
        UUID requesterId,
        UUID voteSessionId,
        String keyword,
        Coordinate center,
        int radius
) implements DomainEvent {

    /** 다른 키와 달리 group. 접두사가 없다. 기존 큐 바인딩 값을 따른다. */
    public static final String TYPE = "store.search.requested";

    @Override
    public String eventType() {
        return TYPE;
    }
}
