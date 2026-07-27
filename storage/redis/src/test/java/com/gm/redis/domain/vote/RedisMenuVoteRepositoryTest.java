package com.gm.redis.domain.vote;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisMenuVoteRepositoryTest {

    @Test
    @DisplayName("모든 후보에 응답한 사용자만 완료 사용자로 계산한다")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void findState_returnsOnlyUsersWhoVotedForEveryCandidate() {
        UUID sessionId = UUID.randomUUID();
        UUID firstCandidateId = UUID.randomUUID();
        UUID secondCandidateId = UUID.randomUUID();
        UUID completedUserId = UUID.randomUUID();
        UUID partialUserId = UUID.randomUUID();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        HashOperations hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        Map<Object, Object> values = new LinkedHashMap<>();
        values.put("status", "OPEN");
        values.put("deadlineEpochMillis", String.valueOf(System.currentTimeMillis() + 60_000));
        values.put("candidateIds", firstCandidateId + "," + secondCandidateId);
        values.put("vote:" + firstCandidateId + ":" + completedUserId, "GO");
        values.put("vote:" + secondCandidateId + ":" + completedUserId, "MAYBE");
        values.put("vote:" + firstCandidateId + ":" + partialUserId, "NO");
        when(hashOperations.entries(VoteRedisKeys.session(sessionId))).thenReturn(values);
        RedisMenuVoteRepository repository = new RedisMenuVoteRepository(
                redisTemplate,
                Duration.ofHours(2),
                Duration.ofHours(24)
        );

        var state = repository.findState(sessionId).orElseThrow();

        assertThat(state.completedUserIds()).containsExactly(completedUserId);
        assertThat(state.counts()).hasSize(2);
    }
}
