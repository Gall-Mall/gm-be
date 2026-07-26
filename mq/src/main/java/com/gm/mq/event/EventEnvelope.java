package com.gm.mq.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 모든 도메인 이벤트의 공통 봉투.
 *
 * <p>eventId는 발행 시점에 생성되며 브로커가 재전달해도 같은 값으로 온다.
 * 중복 발행(API 재호출 등)은 매번 새 eventId가 붙으므로 이것으로 막지 못한다.</p>
 */
public record EventEnvelope<T>(
        // 멱등성을 위한 idempotentKey
        String eventId,
        String eventType,
        // 타임존이 없는 LocalDateTime은 서버·리전이 바뀌면 비교가 깨져 Instant를 쓴다.
        Instant occurredAt,
        T payload
) {
    public static <T> EventEnvelope<T> of(String eventType, T payload) {
        return new EventEnvelope<>(UUID.randomUUID().toString(), eventType, Instant.now(), payload);
    }
}
