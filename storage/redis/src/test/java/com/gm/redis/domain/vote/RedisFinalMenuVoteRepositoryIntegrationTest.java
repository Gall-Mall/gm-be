package com.gm.redis.domain.vote;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.gm.core.domain.vote.candidate.model.FinalMenuVoteResult;

import static org.assertj.core.api.Assertions.assertThat;

class RedisFinalMenuVoteRepositoryIntegrationTest {

    private static final Duration TTL = Duration.ofHours(2);
    private static Process redisProcess;
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedisFinalMenuVoteRepository repository;

    @BeforeAll
    static void startRedis() throws Exception {
        int port = availablePort();
        String redisServer = System.getenv().getOrDefault("REDIS_SERVER_PATH", "redis-server");
        redisProcess = new ProcessBuilder(
                redisServer,
                "--port", String.valueOf(port),
                "--save", "",
                "--appendonly", "no"
        )
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        connectionFactory = new LettuceConnectionFactory("127.0.0.1", port);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        waitUntilReady();
        repository = new RedisFinalMenuVoteRepository(redisTemplate, TTL);
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
    @DisplayName("사용자별 최초 응답과 선택 변경을 집계하고 전원 응답 시 단독 1위를 닫는다")
    void submit_closesWithUniqueWinnerAfterAllEligibleUsersRespond() {
        UUID sessionId = UUID.randomUUID();
        UUID firstCandidate = UUID.randomUUID();
        UUID secondCandidate = UUID.randomUUID();
        UUID firstUser = UUID.randomUUID();
        repository.initialize(sessionId, List.of(firstCandidate, secondCandidate), 3);

        assertThat(repository.submit(sessionId, firstUser, firstCandidate).status())
                .isEqualTo(FinalMenuVoteResult.Status.WAITING);
        assertThat(repository.submit(sessionId, firstUser, secondCandidate).status())
                .isEqualTo(FinalMenuVoteResult.Status.WAITING);
        assertThat(repository.submit(
                sessionId, UUID.randomUUID(), secondCandidate).status())
                .isEqualTo(FinalMenuVoteResult.Status.WAITING);

        FinalMenuVoteResult result = repository.submit(
                sessionId, UUID.randomUUID(), firstCandidate);

        assertThat(result.status()).isEqualTo(FinalMenuVoteResult.Status.SELECTED);
        assertThat(result.selectedCandidateId()).isEqualTo(secondCandidate);
        assertThat(repository.submit(sessionId, firstUser, firstCandidate)).isEqualTo(result);
    }

    @Test
    @DisplayName("전원 응답 결과가 동점이면 방장 선택 가능한 동점 상태를 유지한다")
    void submit_whenTied_keepsTieForOwnerSelection() {
        UUID sessionId = UUID.randomUUID();
        UUID firstCandidate = UUID.randomUUID();
        UUID secondCandidate = UUID.randomUUID();
        repository.initialize(sessionId, List.of(firstCandidate, secondCandidate), 2);
        repository.submit(sessionId, UUID.randomUUID(), firstCandidate);

        FinalMenuVoteResult result = repository.submit(
                sessionId, UUID.randomUUID(), secondCandidate);

        assertThat(result.status()).isEqualTo(FinalMenuVoteResult.Status.TIED);
        assertThat(result.tiedCandidateIds()).containsExactly(firstCandidate, secondCandidate);
        assertThat(repository.isTiedCandidate(sessionId, firstCandidate)).isTrue();
        assertThat(repository.isTiedCandidate(sessionId, secondCandidate)).isTrue();
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
