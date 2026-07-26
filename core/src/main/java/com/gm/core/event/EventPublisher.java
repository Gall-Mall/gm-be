package com.gm.core.event;

/**
 * 도메인 이벤트 발행 포트.
 * 전송 수단은 구현체가 정하며, core는 무엇이 일어났는지만 말한다.
 *
 * <p>트랜잭션 안에서 발행할 때는 AfterCommitExecutor로 감싸야 한다.
 * 그러지 않으면 롤백된 작업의 이벤트가 나간다.</p>
 */
public interface EventPublisher {
    void publish(DomainEvent event);
}
