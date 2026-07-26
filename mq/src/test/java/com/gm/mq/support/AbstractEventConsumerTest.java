package com.gm.mq.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.gm.core.event.ProcessedEventStore;
import com.gm.mq.event.EventEnvelope;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 멱등 판정과 실패 시 되돌리기가 이 변경의 존재 이유이므로 분기별로 검증한다.
 */
class AbstractEventConsumerTest {

    private record Payload(String value) {}

    /** 호출 순서를 기록해 판정이 처리보다 먼저인지 확인한다. */
    private static class RecordingStore implements ProcessedEventStore {
        final List<String> calls = new ArrayList<>();
        boolean markResult = true;
        RuntimeException releaseFailure;

        @Override
        public boolean markIfFirst(String eventId) {
            calls.add("mark");
            return markResult;
        }

        @Override
        public void release(String eventId) {
            calls.add("release");
            if (releaseFailure != null) {
                throw releaseFailure;
            }
        }
    }

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private RecordingStore store;
    private EventConsumer consumer;

    @BeforeEach
    void setUp() {
        store = new RecordingStore();
        consumer = new AbstractEventConsumer(objectMapper, store) {};
    }

    private Message messageOf(EventEnvelope<Payload> envelope) {
        return new Message(objectMapper.writeValueAsBytes(envelope), new MessageProperties());
    }

    private Message validMessage() {
        return messageOf(EventEnvelope.of("test.event", new Payload("hello")));
    }

    @Test
    @DisplayName("처음 보는 이벤트는 판정 후 처리한다")
    void runsTaskForFirstOccurrence() {
        AtomicInteger executed = new AtomicInteger();

        consumer.consumeOnce(validMessage(), Payload.class, payload -> {
            assertThat(payload.value()).isEqualTo("hello");
            executed.incrementAndGet();
        });

        assertThat(executed).hasValue(1);
        // 판정이 처리보다 먼저여야 한다. 순서가 뒤집히면 중복 처리가 새어나간다.
        assertThat(store.calls).containsExactly("mark");
    }

    @Test
    @DisplayName("이미 처리한 이벤트면 task를 실행하지 않는다")
    void skipsDuplicate() {
        store.markResult = false;
        AtomicInteger executed = new AtomicInteger();

        consumer.consumeOnce(validMessage(), Payload.class, payload -> executed.incrementAndGet());

        assertThat(executed).hasValue(0);
        assertThat(store.calls).containsExactly("mark");
    }

    @Test
    @DisplayName("처리 중 RuntimeException이 나면 기록을 되돌리고 예외를 그대로 던진다")
    void releasesOnRuntimeException() {
        RuntimeException failure = new IllegalStateException("업무 실패");

        assertThatThrownBy(() ->
                consumer.consumeOnce(validMessage(), Payload.class, payload -> { throw failure; }))
                .isSameAs(failure);

        assertThat(store.calls).containsExactly("mark", "release");
    }

    @Test
    @DisplayName("처리 중 Error가 나도 기록을 되돌린다")
    void releasesOnError() {
        // RuntimeException만 잡으면 여기서 기록이 남아 재전달분이 영영 스킵된다.
        assertThatThrownBy(() ->
                consumer.consumeOnce(validMessage(), Payload.class, payload -> {
                    throw new StackOverflowError("치명적 실패");
                }))
                .isInstanceOf(StackOverflowError.class);

        assertThat(store.calls).containsExactly("mark", "release");
    }

    @Test
    @DisplayName("되돌리기가 실패해도 원래 예외를 덮어쓰지 않는다")
    void releaseFailureDoesNotMaskOriginalException() {
        RuntimeException original = new IllegalStateException("업무 실패");
        store.releaseFailure = new IllegalStateException("Redis 끊김");

        assertThatThrownBy(() ->
                consumer.consumeOnce(validMessage(), Payload.class, payload -> { throw original; }))
                .isSameAs(original);
    }

    @Test
    @DisplayName("eventId가 없으면 처리하지 않고 즉시 DLQ 대상으로 던진다")
    void rejectsEnvelopeWithoutEventId() {
        Message message = messageOf(
                new EventEnvelope<>(null, "test.event", Instant.now(), new Payload("hello")));
        AtomicInteger executed = new AtomicInteger();

        assertThatThrownBy(() ->
                consumer.consumeOnce(message, Payload.class, payload -> executed.incrementAndGet()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(executed).hasValue(0);
        assertThat(store.calls).isEmpty();
    }

    @Test
    @DisplayName("본문이 깨졌으면 판정 전에 즉시 DLQ 대상으로 던진다")
    void rejectsMalformedBody() {
        Message message = new Message("깨진 본문".getBytes(), new MessageProperties());

        assertThatThrownBy(() -> consumer.consumeOnce(message, Payload.class, payload -> { }))
                .isInstanceOf(IllegalArgumentException.class);

        // 역직렬화가 실패했으므로 기록도 남지 않아야 한다.
        assertThat(store.calls).isEmpty();
    }

    @Test
    @DisplayName("payload를 선언한 타입으로 복원한다")
    void deserializesPayloadIntoDeclaredType() {
        String value = UUID.randomUUID().toString();
        Message message = messageOf(EventEnvelope.of("test.event", new Payload(value)));

        consumer.consumeOnce(message, Payload.class, payload ->
                // 타입 소거 때문에 LinkedHashMap이 넘어오면 여기서 깨진다.
                assertThat(payload.value()).isEqualTo(value));
    }
}
