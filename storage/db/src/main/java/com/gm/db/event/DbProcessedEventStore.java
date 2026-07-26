package com.gm.db.event;

import java.time.Instant;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.gm.core.event.ProcessedEventStore;

/**
 * inbox — 처리 기록을 비즈니스 로직과 같은 트랜잭션에 남긴다.
 *
 * <p>@Transactional을 붙이지 않는다. 호출자가 연 트랜잭션에 그대로 참여해야 inbox가 성립한다.</p>
 */
@Component("dbProcessedEventStore")
@RequiredArgsConstructor
public class DbProcessedEventStore implements ProcessedEventStore {

    private final ProcessedEventJpaRepository repository;

    /** 원자적 INSERT로 판정한다. 동시에 들어와도 한쪽만 1을 받는다. */
    @Override
    public boolean markIfFirst(String eventId) {
        return repository.insertIfAbsent(eventId, Instant.now()) == 1;
    }

    @Override
    public void release(String eventId) {
        // 롤백이 INSERT를 함께 지우므로 되돌릴 것이 없다.
    }
}
