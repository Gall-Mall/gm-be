package com.gm.redis.event;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * dedup 판정이 원자적인지, release가 실제로 키를 지우는지 확인한다.
 */
class RedisProcessedEventStoreIntegrationTest {

    private static final String KEY_PREFIX = "mq:evt:";

    private static Process redisProcess;
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedisProcessedEventStore store;

    @BeforeAll
    static void startRedis() throws Exception {
        int port = availablePort();
        String redisServer = System.getenv().getOrDefault("REDIS_SERVER_PATH", "redis-server");
        try {
            redisProcess = new ProcessBuilder(
                    redisServer,
                    "--port", String.valueOf(port),
                    "--save", "",
                    "--appendonly", "no"
            )
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException e) {
            // 실행 파일이 없는 환경에서 빨간 빌드를 만들지 않는다. REDIS_SERVER_PATH로 지정 가능.
            Assumptions.abort("redis-server를 찾지 못해 건너뛴다: " + redisServer);
        }

        connectionFactory = new LettuceConnectionFactory("127.0.0.1", port);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        waitUntilReady();
        store = new RedisProcessedEventStore(redisTemplate);
    }

    @AfterEach
    void flushRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @AfterAll
    static void stopRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        if (redisProcess != null) {
            redisProcess.destroy();
        }
    }

    @Test
    @DisplayName("처음 보는 eventId만 통과시키고 TTL을 건다")
    void marksOnlyFirstOccurrence() {
        String eventId = UUID.randomUUID().toString();

        assertThat(store.markIfFirst(eventId)).isTrue();
        assertThat(store.markIfFirst(eventId)).isFalse();
        // TTL이 없으면 키가 영구히 남아 DLQ 재처리까지 막는다.
        assertThat(redisTemplate.getExpire(KEY_PREFIX + eventId)).isPositive();
    }

    @Test
    @DisplayName("동시에 같은 eventId가 들어와도 하나만 통과한다")
    void marksOnlyOnceUnderConcurrency() throws Exception {
        String eventId = UUID.randomUUID().toString();
        int threads = 8;

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            Callable<Boolean> attempt = () -> {
                start.await(5, TimeUnit.SECONDS);
                return store.markIfFirst(eventId);
            };

            List<Future<Boolean>> futures = java.util.stream.IntStream.range(0, threads)
                    .mapToObj(i -> executor.submit(attempt))
                    .toList();
            start.countDown();

            long passed = 0;
            for (Future<Boolean> future : futures) {
                if (Boolean.TRUE.equals(future.get(10, TimeUnit.SECONDS))) {
                    passed++;
                }
            }

            assertThat(passed).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("release 후에는 다시 처음으로 판정한다")
    void releaseAllowsReprocessing() {
        String eventId = UUID.randomUUID().toString();

        assertThat(store.markIfFirst(eventId)).isTrue();
        store.release(eventId);

        // 되돌리지 않으면 재시도가 전부 스킵되므로 이 동작이 유실 방지의 핵심이다.
        assertThat(store.markIfFirst(eventId)).isTrue();
    }

    @Test
    @DisplayName("기록에 없는 eventId를 release해도 예외가 나지 않는다")
    void releaseIsSafeForUnknownEventId() {
        store.release(UUID.randomUUID().toString());
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void waitUntilReady() throws InterruptedException {
        for (int attempt = 0; attempt < 50; attempt++) {
            try {
                redisTemplate.getConnectionFactory().getConnection().ping();
                return;
            } catch (RuntimeException e) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("테스트용 Redis 기동 실패");
    }
}
