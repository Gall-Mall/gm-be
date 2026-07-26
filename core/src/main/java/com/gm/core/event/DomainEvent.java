package com.gm.core.event;

/**
 * 발행 가능한 도메인 이벤트.
 * eventType은 도메인 어휘이며, 이를 라우팅 키로 쓸지는 mq가 정한다.
 */
public interface DomainEvent {
    String eventType();
}
