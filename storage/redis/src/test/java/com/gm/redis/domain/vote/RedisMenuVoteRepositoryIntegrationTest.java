package com.gm.redis.domain.vote;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.gm.core.domain.vote.candidate.exception.VoteCandidateErrorCode;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateException;
import com.gm.core.domain.vote.candidate.model.MenuVoteChoice;
import com.gm.core.domain.vote.candidate.model.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.MenuVoteSession;
import com.gm.core.domain.vote.candidate.model.MenuVoteSubmission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisMenuVoteRepositoryIntegrationTest {

    private static final Duration DEFAULT_TTL = Duration.ofHours(2);
    private static final Duration CLOSED_RECOVERY_TTL = Duration.ofHours(24);
    private static final Duration VOTING_DURATION = Duration.ofMinutes(30);

    private static Process redisProcess;
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedisMenuVoteRepository repository;

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
        repository = new RedisMenuVoteRepository(redisTemplate, DEFAULT_TTL, CLOSED_RECOVERY_TTL);
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
    @DisplayName("세션 메타데이터와 후보 소속을 단일 Hash에 저장하고 기본 TTL을 설정한다")
    void initialize_storesImmutableMetadataInSingleHash() {
        UUID sessionId = UUID.randomUUID();
        UUID firstCandidateId = UUID.randomUUID();
        UUID secondCandidateId = UUID.randomUUID();
        long before = System.currentTimeMillis();

        repository.initialize(session(sessionId, List.of(firstCandidateId, secondCandidateId)));

        String key = VoteRedisKeys.session(sessionId);
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(key);
        long deadline = Long.parseLong(fields.get("deadlineEpochMillis").toString());
        assertThat(redisTemplate.keys("menu-vote:*")).containsExactly(key);
        assertThat(fields)
                .containsEntry("sessionId", sessionId.toString())
                .containsEntry("status", "OPEN")
                .containsEntry("candidateIds", firstCandidateId + "," + secondCandidateId)
                .containsEntry("candidate:" + firstCandidateId, "1")
                .containsEntry("candidate:" + secondCandidateId, "1");
        assertThat(deadline).isBetween(
                before + VOTING_DURATION.toMillis(),
                System.currentTimeMillis() + VOTING_DURATION.toMillis()
        );
        assertThat(redisTemplate.getExpire(key)).isBetween(
                DEFAULT_TTL.toSeconds() - 2,
                DEFAULT_TTL.toSeconds()
        );
    }

    @Test
    @DisplayName("설정 TTL이 투표 시간보다 짧아도 만료 시각은 마감 시각보다 뒤다")
    void initialize_keepsEffectiveTtlPastDeadline() {
        RedisMenuVoteRepository shortTtlRepository =
                new RedisMenuVoteRepository(redisTemplate, Duration.ofMinutes(1), CLOSED_RECOVERY_TTL);
        UUID sessionId = UUID.randomUUID();

        shortTtlRepository.initialize(session(sessionId, List.of(UUID.randomUUID())));

        assertThat(redisTemplate.getExpire(VoteRedisKeys.session(sessionId)))
                .isGreaterThan(VOTING_DURATION.toSeconds());
    }

    @Test
    @DisplayName("OPEN 재초기화는 불변 메타데이터를 바꾸지 않고 CLOSED 세션을 다시 열지 않는다")
    void initialize_isIdempotentAndNeverReopensClosedSession() {
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        repository.initialize(session(sessionId, List.of(candidateId)));
        String key = VoteRedisKeys.session(sessionId);
        Map<Object, Object> initialized = redisTemplate.opsForHash().entries(key);

        repository.initialize(session(sessionId, List.of(UUID.randomUUID())));
        assertThat(redisTemplate.opsForHash().entries(key)).isEqualTo(initialized);

        repository.closeAndGetSnapshot(sessionId);
        repository.initialize(session(sessionId, List.of(candidateId)));
        assertThat(redisTemplate.opsForHash().get(key, "status")).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("첫 선택과 선택 변경을 원자적으로 집계하고 동일 선택은 중복 집계하지 않는다")
    void submit_createsChangesAndDeduplicatesVote() {
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        repository.initialize(session(sessionId, List.of(candidateId)));

        MenuVoteSubmission first = repository.submit(sessionId, candidateId, userId, MenuVoteChoice.GO);
        MenuVoteSubmission duplicate = repository.submit(sessionId, candidateId, userId, MenuVoteChoice.GO);
        MenuVoteSubmission changed = repository.submit(sessionId, candidateId, userId, MenuVoteChoice.MAYBE);

        assertThat(first.changed()).isTrue();
        assertThat(first.count()).isEqualTo(new MenuVoteCount(candidateId, 1, 0, 0, 1));
        assertThat(duplicate.changed()).isFalse();
        assertThat(duplicate.count()).isEqualTo(new MenuVoteCount(candidateId, 1, 0, 0, 1));
        assertThat(changed.changed()).isTrue();
        assertThat(changed.count()).isEqualTo(new MenuVoteCount(candidateId, 0, 1, 0, 1));
    }

    @Test
    @DisplayName("세션에 소속되지 않은 후보의 투표를 원자 연산 안에서 거절한다")
    void submit_rejectsCandidateOutsideSession() {
        UUID sessionId = UUID.randomUUID();
        repository.initialize(session(sessionId, List.of(UUID.randomUUID())));

        assertThatThrownBy(() -> repository.submit(
                sessionId, UUID.randomUUID(), UUID.randomUUID(), MenuVoteChoice.GO
        ))
                .isInstanceOf(VoteCandidateException.class)
                .extracting(exception -> ((VoteCandidateException) exception).getErrorCode())
                .isEqualTo(VoteCandidateErrorCode.CANDIDATE_NOT_FOUND);
    }

    @Test
    @DisplayName("Redis 서버 시간이 마감 시각을 지나면 투표를 거절하고 세션을 닫는다")
    void submit_closesSessionAfterDeadline() throws Exception {
        RedisMenuVoteRepository expiringRepository =
                new RedisMenuVoteRepository(redisTemplate, DEFAULT_TTL, CLOSED_RECOVERY_TTL);
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        expiringRepository.initialize(new MenuVoteSession(
                sessionId, List.of(candidateId), Duration.ofMillis(50)
        ));
        Thread.sleep(100);

        assertThatThrownBy(() -> expiringRepository.submit(
                sessionId, candidateId, UUID.randomUUID(), MenuVoteChoice.GO
        ))
                .isInstanceOf(VoteCandidateException.class)
                .extracting(exception -> ((VoteCandidateException) exception).getErrorCode())
                .isEqualTo(VoteCandidateErrorCode.VOTE_ALREADY_CLOSED);
        assertThat(redisTemplate.opsForHash().get(VoteRedisKeys.session(sessionId), "status"))
                .isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("마감 후 첫 제출이 세션을 닫으면 복구 TTL과 스냅샷을 유지한다")
    void submit_afterDeadlineRefreshesRecoveryTtlAndKeepsSnapshotForRetry() throws Exception {
        Duration openTtl = Duration.ofSeconds(5);
        RedisMenuVoteRepository shortTtlRepository =
                new RedisMenuVoteRepository(redisTemplate, openTtl, CLOSED_RECOVERY_TTL);
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        String key = VoteRedisKeys.session(sessionId);
        shortTtlRepository.initialize(new MenuVoteSession(
                sessionId, List.of(candidateId), Duration.ofMillis(50)
        ));
        shortTtlRepository.submit(sessionId, candidateId, UUID.randomUUID(), MenuVoteChoice.GO);
        redisTemplate.opsForHash().put(key, "deadlineEpochMillis", "0");
        redisTemplate.expire(key, openTtl);

        assertThatThrownBy(() -> shortTtlRepository.submit(
                sessionId, candidateId, UUID.randomUUID(), MenuVoteChoice.NO
        ))
                .isInstanceOf(VoteCandidateException.class)
                .extracting(exception -> ((VoteCandidateException) exception).getErrorCode())
                .isEqualTo(VoteCandidateErrorCode.VOTE_ALREADY_CLOSED);
        assertThat(redisTemplate.opsForHash().get(key, "status")).isEqualTo("CLOSED");
        assertThat(redisTemplate.getExpire(key)).isBetween(
                CLOSED_RECOVERY_TTL.toSeconds() - 2,
                CLOSED_RECOVERY_TTL.toSeconds()
        );

        Thread.sleep(openTtl.plusMillis(100).toMillis());
        assertThat(redisTemplate.hasKey(key)).isTrue();

        redisTemplate.expire(key, Duration.ofMinutes(1));
        assertThat(shortTtlRepository.closeAndGetSnapshot(sessionId))
                .containsExactly(new MenuVoteCount(candidateId, 1, 0, 0, 1));
        assertThat(redisTemplate.getExpire(key)).isBetween(
                CLOSED_RECOVERY_TTL.toSeconds() - 2,
                CLOSED_RECOVERY_TTL.toSeconds()
        );
    }

    @Test
    @DisplayName("응답자가 없는 수동 마감은 원자적으로 거절하고 투표를 계속 열어 둔다")
    void closeManually_withoutResponse_keepsSessionOpen() {
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        repository.initialize(session(sessionId, List.of(candidateId)));

        assertThatThrownBy(() -> repository.closeAndGetSnapshotIfAnyResponse(sessionId))
                .isInstanceOf(VoteCandidateException.class)
                .extracting(exception -> ((VoteCandidateException) exception).getErrorCode())
                .isEqualTo(VoteCandidateErrorCode.VOTE_CLOSE_NOT_ALLOWED);

        assertThat(redisTemplate.opsForHash().get(VoteRedisKeys.session(sessionId), "status"))
                .isEqualTo("OPEN");
        assertThat(repository.submit(sessionId, candidateId, UUID.randomUUID(), MenuVoteChoice.GO).changed())
                .isTrue();
    }

    @Test
    @DisplayName("마감은 후보 순서가 고정된 스냅샷을 원자적으로 만들고 재시도에도 그대로 반환한다")
    void close_blocksSubmissionAndKeepsStableSnapshotForRetry() {
        UUID sessionId = UUID.randomUUID();
        UUID firstCandidateId = UUID.randomUUID();
        UUID secondCandidateId = UUID.randomUUID();
        repository.initialize(session(sessionId, List.of(firstCandidateId, secondCandidateId)));
        repository.submit(sessionId, secondCandidateId, UUID.randomUUID(), MenuVoteChoice.NO);

        List<MenuVoteCount> first = repository.closeAndGetSnapshot(sessionId);
        List<MenuVoteCount> retry = repository.closeAndGetSnapshot(sessionId);

        assertThat(first).containsExactly(
                new MenuVoteCount(firstCandidateId, 0, 0, 0, 0),
                new MenuVoteCount(secondCandidateId, 0, 0, 1, 1)
        );
        assertThat(retry).isEqualTo(first);
        assertThatThrownBy(() -> repository.submit(
                sessionId, firstCandidateId, UUID.randomUUID(), MenuVoteChoice.GO
        )).isInstanceOf(VoteCandidateException.class);
    }

    @Test
    @DisplayName("마감과 마감 재시도는 스냅샷을 유지하며 복구 보관 TTL을 새로 설정한다")
    void close_refreshesRecoveryTtlAndKeepsStableSnapshotForRetry() {
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        repository.initialize(session(sessionId, List.of(candidateId)));
        repository.submit(sessionId, candidateId, UUID.randomUUID(), MenuVoteChoice.GO);
        String key = VoteRedisKeys.session(sessionId);
        redisTemplate.expire(key, Duration.ofMinutes(1));

        List<MenuVoteCount> first = repository.closeAndGetSnapshot(sessionId);

        assertThat(first).containsExactly(new MenuVoteCount(candidateId, 1, 0, 0, 1));
        assertThat(redisTemplate.getExpire(key)).isBetween(
                CLOSED_RECOVERY_TTL.toSeconds() - 2,
                CLOSED_RECOVERY_TTL.toSeconds()
        );

        redisTemplate.expire(key, Duration.ofMinutes(1));
        List<MenuVoteCount> retry = repository.closeAndGetSnapshot(sessionId);

        assertThat(retry).isEqualTo(first);
        assertThat(redisTemplate.getExpire(key)).isBetween(
                CLOSED_RECOVERY_TTL.toSeconds() - 2,
                CLOSED_RECOVERY_TTL.toSeconds()
        );
        assertThat(redisTemplate.keys("menu-vote:*")).containsExactly(key);
    }

    @Test
    @DisplayName("동시에 들어온 서로 다른 사용자 투표를 유실하지 않는다")
    void submit_keepsConcurrentVotes() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        repository.initialize(session(sessionId, List.of(candidateId)));
        ExecutorService executor = Executors.newFixedThreadPool(8);

        IntStream.range(0, 40).forEach(index -> executor.submit(() -> repository.submit(
                sessionId,
                candidateId,
                UUID.randomUUID(),
                index % 2 == 0 ? MenuVoteChoice.GO : MenuVoteChoice.MAYBE
        )));
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(repository.closeAndGetSnapshot(sessionId))
                .containsExactly(new MenuVoteCount(candidateId, 20, 20, 0, 40));
    }

    @Test
    @DisplayName("DB 저장 완료 후 세션의 단일 Hash를 삭제한다")
    void delete_removesSessionHash() {
        UUID sessionId = UUID.randomUUID();
        repository.initialize(session(sessionId, List.of(UUID.randomUUID())));

        repository.delete(sessionId);

        assertThat(redisTemplate.hasKey(VoteRedisKeys.session(sessionId))).isFalse();
    }

    private static MenuVoteSession session(UUID sessionId, List<UUID> candidateIds) {
        return new MenuVoteSession(sessionId, candidateIds, VOTING_DURATION);
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