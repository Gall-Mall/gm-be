package com.gm.integration.event;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.gm.api.ApiApplication;
import com.gm.core.event.ProcessedEventStore;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * inbox 판정이 처리보다 먼저, 그리고 동시 요청에서도 한 번만 통과하는지 확인한다.
 * save()는 INSERT를 커밋까지 미뤄 두 트랜잭션이 모두 통과할 수 있어 네이티브 INSERT를 쓴다.
 */
@SpringBootTest(classes = ApiApplication.class)
class DbProcessedEventStoreIntegrationTest {

    @Autowired
    @Qualifier("dbProcessedEventStore")
    private ProcessedEventStore store;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> createdEventIds = new java.util.ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdEventIds.forEach(id ->
                jdbcTemplate.update("DELETE FROM processed_events WHERE event_id = ?", id));
        createdEventIds.clear();
    }

    @Test
    @DisplayName("같은 트랜잭션 안에서 두 번째 호출은 커밋 전에도 걸러진다")
    void marksOnlyFirstOccurrenceWithinTransaction() {
        String eventId = newEventId();

        Boolean secondResult = transactionTemplate.execute(status -> {
            assertThat(store.markIfFirst(eventId)).isTrue();
            // 커밋 전인데도 걸러져야 판정이 처리보다 앞선다고 볼 수 있다.
            return store.markIfFirst(eventId);
        });

        assertThat(secondResult).isFalse();
    }

    @Test
    @DisplayName("서로 다른 트랜잭션이 동시에 같은 eventId를 넣으면 하나만 통과한다")
    void marksOnlyFirstOccurrenceAcrossConcurrentTransactions() throws Exception {
        String eventId = newEventId();
        int threads = 2;

        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            Callable<Boolean> attempt = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                TransactionTemplate template = new TransactionTemplate(transactionTemplate.getTransactionManager());
                template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                return template.execute(status -> store.markIfFirst(eventId));
            };

            Future<Boolean> first = executor.submit(attempt);
            Future<Boolean> second = executor.submit(attempt);

            ready.await(5, TimeUnit.SECONDS);
            start.countDown();

            long passed = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))
                    .stream()
                    .filter(Boolean.TRUE::equals)
                    .count();

            assertThat(passed).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("서로 다른 eventId는 각각 처음으로 판정한다")
    void marksDistinctEventsIndependently() {
        transactionTemplate.executeWithoutResult(status -> {
            assertThat(store.markIfFirst(newEventId())).isTrue();
            assertThat(store.markIfFirst(newEventId())).isTrue();
        });
    }

    private String newEventId() {
        String eventId = UUID.randomUUID().toString();
        createdEventIds.add(eventId);
        return eventId;
    }
}
