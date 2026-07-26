package com.gm.mq.support;

import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;

import com.gm.core.event.ProcessedEventStore;
import com.gm.mq.event.EventEnvelope;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/**
 * 역직렬화와 멱등 판정의 공통 뼈대. 트랜잭션 경계만 하위 클래스가 정한다.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractEventConsumer implements EventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventStore store;

    @Override
    public <T> void consumeOnce(Message message, Class<T> payloadType, Consumer<T> task) {
        EventEnvelope<T> envelope = read(message, payloadType);
        String eventId = envelope.eventId();

        if (eventId == null || eventId.isBlank()) {
            // 식별자가 없으면 멱등 판정이 불가능하다. 그대로 처리하면 중복이 조용히 새므로,
            // 즉시 DLQ로 보내 사람이 보게 한다. 정상 발행 경로에서는 나올 수 없는 상태다.
            throw new IllegalArgumentException(
                    "eventId 없는 이벤트: " + envelope.eventType());
        }

        // 판정은 반드시 실제 처리보다 먼저. 처리 후 기록하면 그 사이에 죽었을 때 중복 처리된다.
        // DB 구현체는 네이티브 INSERT라 영속성 컨텍스트를 우회한다. 이 호출을 다른 repository
        // 작업 뒤로 옮기면 flush 순서가 어긋나므로 순서를 바꾸지 말 것.
        if (!store.markIfFirst(eventId)) {
            log.info("[mq] 중복 이벤트 스킵: {} / {}", envelope.eventType(), eventId);
            return;
        }

        // RuntimeException만 잡으면 Error가 났을 때 기록이 남아 재전달분이 영영 스킵된다.
        boolean succeeded = false;
        try {
            task.accept(envelope.payload());
            succeeded = true;
        } finally {
            if (!succeeded) {
                release(eventId);
            }
        }
    }

    /**
     * release 실패가 원래 예외를 덮어쓰지 않도록 삼키고 로그만 남긴다.
     * finally에서 부르므로 Error까지 잡지 않으면 여기서 원래 예외가 사라진다.
     */
    private void release(String eventId) {
        try {
            store.release(eventId);
        } catch (Throwable t) {
            log.error("[mq] 처리 기록 되돌리기 실패 — 이 eventId는 재전달돼도 스킵된다: {}", eventId, t);
        }
    }

    /** 제네릭 payload는 타입 소거로 컨버터가 복원하지 못하므로 타입을 명시해 직접 읽는다. */
    private <T> EventEnvelope<T> read(Message message, Class<T> payloadType) {
        JavaType type = objectMapper.getTypeFactory()
                .constructParametricType(EventEnvelope.class, payloadType);
        try {
            return objectMapper.readValue(message.getBody(), type);
        } catch (JacksonException e) {
            // 재시도해도 같은 본문이므로 즉시 DLQ 대상으로 분류되게 한다.
            throw new IllegalArgumentException("이벤트 역직렬화 실패: " + payloadType.getSimpleName(), e);
        }
    }
}
