package com.gm.core.event;

/**
 * 이미 처리한 이벤트를 기록해 중복 소비를 막는다.
 * 구현체가 redis·db 양쪽에 있어야 해서 포트를 core에 둔다.
 */
public interface ProcessedEventStore {

    /** 처음 보는 eventId일 때만 기록하고 true. 확인과 기록은 원자적이어야 한다. */
    boolean markIfFirst(String eventId);

    /** 처리 실패 시 기록을 되돌린다. 트랜잭션 기반 구현체는 롤백이 대신하므로 no-op. */
    void release(String eventId);
}
