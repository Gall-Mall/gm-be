package com.gm.redis.domain.vote;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.gm.core.domain.vote.candidate.model.FinalMenuVoteCloseResult;

import static org.assertj.core.api.Assertions.assertThat;

class RedisFinalMenuVoteExpirationIntegrationTest {

    private static final Duration TTL = Duration.ofHours(2);
    private static final Duration VOTING_DURATION = Duration.ofMillis(100);
    private static Process redisProcess;
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedisFinalMenuVoteRepository repository;

    @BeforeAll
    static void startRedis() throws Exception {
        int port = availablePort();
        redisProcess = new ProcessBuilder(
                System.getenv().getOrDefault("REDIS_SERVER_PATH", "redis-server"),
                "--port", String.valueOf(port), "--save", "", "--appendonly", "no")
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        connectionFactory = new LettuceConnectionFactory("127.0.0.1", port);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        waitUntilReady();
        repository = new RedisFinalMenuVoteRepository(redisTemplate, TTL, VOTING_DURATION);
    }

    @AfterEach
    void flushRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @AfterAll
    static void stopRedis() {
        if (connectionFactory != null) connectionFactory.destroy();
        if (redisProcess != null) redisProcess.destroy();
    }

    @Test
    @DisplayName("deadline 이전에는 최종투표를 마감하지 않는다")
    void closeExpired_beforeDeadline_doesNotClose() {
        UUID sessionId = UUID.randomUUID();
        repository.initialize(sessionId, List.of(UUID.randomUUID(), UUID.randomUUID()), 3);

        assertThat(repository.closeExpired(sessionId).status())
                .isEqualTo(FinalMenuVoteCloseResult.Status.NOT_DUE);
    }

    @Test
    @DisplayName("만료 인덱스의 세션을 조회하고 응답이 있는 단독 1위를 원자적으로 마감한다")
    void closeExpired_withUniqueWinner_selectsWinner() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID winner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        repository.initialize(sessionId, List.of(winner, other), 3);
        repository.submit(sessionId, UUID.randomUUID(), winner);
        Thread.sleep(VOTING_DURATION.plusMillis(40).toMillis());

        assertThat(repository.findExpired(Instant.now(), 10)).contains(sessionId);
        FinalMenuVoteCloseResult result = repository.closeExpired(sessionId);

        assertThat(result.status()).isEqualTo(FinalMenuVoteCloseResult.Status.UNIQUE_WINNER);
        assertThat(result.selectedCandidateId()).isEqualTo(winner);
    }

    @Test
    @DisplayName("단독 1위가 조기 확정돼도 DB 반영 전까지 만료 인덱스에서 복구할 수 있다")
    void selectedBeforeDeadline_remainsRecoverable() {
        UUID sessionId = UUID.randomUUID();
        UUID winner = UUID.randomUUID();
        repository.initialize(sessionId, List.of(winner, UUID.randomUUID()), 1);

        repository.submit(sessionId, UUID.randomUUID(), winner);

        assertThat(repository.findExpired(Instant.now().plusSeconds(1), 10))
                .contains(sessionId);
        FinalMenuVoteCloseResult result = repository.closeExpired(sessionId);
        assertThat(result.status()).isEqualTo(FinalMenuVoteCloseResult.Status.UNIQUE_WINNER);
        assertThat(result.selectedCandidateId()).isEqualTo(winner);
    }

    @Test
    @DisplayName("응답 없이 만료되면 방장 선택 대기 상태로 닫는다")
    void closeExpired_withoutResponse_waitsForOwner() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        repository.initialize(sessionId, List.of(first, second), 3);
        Thread.sleep(VOTING_DURATION.plusMillis(40).toMillis());

        FinalMenuVoteCloseResult result = repository.closeExpired(sessionId);

        assertThat(result.status())
                .isEqualTo(FinalMenuVoteCloseResult.Status.OWNER_SELECTION_PENDING);
        assertThat(repository.isTiedCandidate(sessionId, first)).isTrue();
        assertThat(repository.isTiedCandidate(sessionId, second)).isTrue();
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void waitUntilReady() throws InterruptedException {
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            try {
                redisTemplate.getConnectionFactory().getConnection().ping();
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                Thread.sleep(100);
            }
        }
        throw lastFailure == null ? new IllegalStateException("Redis did not start") : lastFailure;
    }
}
