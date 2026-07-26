package com.gm.db.event;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, String> {

    /**
     * 처음 보는 eventId면 1, 이미 있으면 0을 반환한다.
     *
     * <p>save()는 INSERT를 커밋 시점까지 미루므로 비즈니스 로직이 끝난 뒤에야 중복이 드러난다.
     * 판정을 처리 전에 끝내려면 즉시 실행되는 원자적 INSERT가 필요하다.
     * 충돌 시 예외 대신 0을 반환해야 트랜잭션이 rollback-only로 오염되지 않는다.</p>
     *
     * <p>ON DUPLICATE KEY UPDATE는 쓸 수 없다. Connector/J가 CLIENT_FOUND_ROWS로 붙어
     * 변경 행이 아니라 매칭 행을 세므로 중복일 때도 1이 돌아온다.</p>
     */
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO processed_events (event_id, processed_at)
            VALUES (:eventId, :processedAt)
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") String eventId, @Param("processedAt") Instant processedAt);
}
