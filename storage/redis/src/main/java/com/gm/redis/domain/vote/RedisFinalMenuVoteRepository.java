package com.gm.redis.domain.vote;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import com.gm.core.domain.vote.candidate.exception.VoteCandidateErrorCode;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateException;
import com.gm.core.domain.vote.candidate.model.finalmenu.FinalMenuVoteCloseResult;
import com.gm.core.domain.vote.candidate.model.finalmenu.FinalMenuVoteCount;
import com.gm.core.domain.vote.candidate.model.finalmenu.FinalMenuVoteResult;
import com.gm.core.domain.vote.candidate.model.finalmenu.FinalMenuVoteState;
import com.gm.core.domain.vote.candidate.repository.finalmenu.FinalMenuVoteRepository;
import com.gm.core.domain.vote.candidate.service.menuvote.MenuVotePolicy;

/** 두 후보 최종 투표와 deadline 인덱스를 Redis Hash·Sorted Set·Lua로 관리한다. */
@Repository
public class RedisFinalMenuVoteRepository implements FinalMenuVoteRepository {

    private static final String DEADLINE_INDEX = "final-menu-vote:deadlines";

    private static final DefaultRedisScript<Long> INITIALIZE_SCRIPT = script(
            """
            if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
            local redisTime = redis.call('TIME')
            local nowMillis = (tonumber(redisTime[1]) * 1000) + math.floor(tonumber(redisTime[2]) / 1000)
            local deadlineMillis = nowMillis + tonumber(ARGV[4])
            redis.call('HSET', KEYS[1],
                'status', 'OPEN',
                'candidate:1', ARGV[1],
                'candidate:2', ARGV[2],
                'eligibleVoterCount', ARGV[3],
                'respondedCount', '0',
                'deadlineEpochMillis', tostring(deadlineMillis),
                'count:' .. ARGV[1], '0',
                'count:' .. ARGV[2], '0')
            redis.call('PEXPIRE', KEYS[1], ARGV[5])
            redis.call('ZADD', KEYS[2], deadlineMillis, ARGV[6])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<List> SUBMIT_SCRIPT = script(
            """
            local status = redis.call('HGET', KEYS[1], 'status')
            if not status then return {-1} end
            if status == 'SELECTED' then
                local selectedTime = redis.call('TIME')
                local selectedAt = (tonumber(selectedTime[1]) * 1000) + math.floor(tonumber(selectedTime[2]) / 1000)
                redis.call('ZADD', KEYS[2], selectedAt, ARGV[3])
                return {2, redis.call('HGET', KEYS[1], 'selectedCandidateId')}
            end
            if status == 'OWNER_SELECTION_PENDING' then
                return {3, redis.call('HGET', KEYS[1], 'candidate:1'), redis.call('HGET', KEYS[1], 'candidate:2')}
            end
            local first = redis.call('HGET', KEYS[1], 'candidate:1')
            local second = redis.call('HGET', KEYS[1], 'candidate:2')
            local redisTime = redis.call('TIME')
            local nowMillis = (tonumber(redisTime[1]) * 1000) + math.floor(tonumber(redisTime[2]) / 1000)
            local deadline = tonumber(redis.call('HGET', KEYS[1], 'deadlineEpochMillis'))
            if nowMillis >= deadline then
                local responded = tonumber(redis.call('HGET', KEYS[1], 'respondedCount') or '0')
                local firstCount = tonumber(redis.call('HGET', KEYS[1], 'count:' .. first) or '0')
                local secondCount = tonumber(redis.call('HGET', KEYS[1], 'count:' .. second) or '0')
                if responded > 0 and firstCount ~= secondCount then
                    local winner = firstCount > secondCount and first or second
                    redis.call('HSET', KEYS[1], 'status', 'SELECTED', 'selectedCandidateId', winner)
                    redis.call('ZADD', KEYS[2], nowMillis, ARGV[3])
                    return {2, winner}
                end
                redis.call('ZREM', KEYS[2], ARGV[3])
                redis.call('HSET', KEYS[1], 'status', 'OWNER_SELECTION_PENDING')
                return {3, first, second}
            end
            if ARGV[2] ~= first and ARGV[2] ~= second then return {-2} end
            local voteField = 'vote:' .. ARGV[1]
            local oldCandidate = redis.call('HGET', KEYS[1], voteField)
            if oldCandidate ~= ARGV[2] then
                if oldCandidate then
                    redis.call('HINCRBY', KEYS[1], 'count:' .. oldCandidate, -1)
                else
                    redis.call('HINCRBY', KEYS[1], 'respondedCount', 1)
                end
                redis.call('HINCRBY', KEYS[1], 'count:' .. ARGV[2], 1)
                redis.call('HSET', KEYS[1], voteField, ARGV[2])
            end
            local responded = tonumber(redis.call('HGET', KEYS[1], 'respondedCount'))
            local eligible = tonumber(redis.call('HGET', KEYS[1], 'eligibleVoterCount'))
            if responded < eligible then return {1} end
            local firstCount = tonumber(redis.call('HGET', KEYS[1], 'count:' .. first))
            local secondCount = tonumber(redis.call('HGET', KEYS[1], 'count:' .. second))
            if firstCount == secondCount then
                redis.call('ZREM', KEYS[2], ARGV[3])
                redis.call('HSET', KEYS[1], 'status', 'OWNER_SELECTION_PENDING')
                return {3, first, second}
            end
            local winner = firstCount > secondCount and first or second
            redis.call('HSET', KEYS[1], 'status', 'SELECTED', 'selectedCandidateId', winner)
            redis.call('ZADD', KEYS[2], nowMillis, ARGV[3])
            return {2, winner}
            """, List.class);

    private static final DefaultRedisScript<List> CLOSE_EXPIRED_SCRIPT = script(
            """
            local status = redis.call('HGET', KEYS[1], 'status')
            if not status then return {-1} end
            if status == 'SELECTED' then
                return {2, redis.call('HGET', KEYS[1], 'selectedCandidateId')}
            end
            if status == 'OWNER_SELECTION_PENDING' then return {3} end
            local redisTime = redis.call('TIME')
            local nowMillis = (tonumber(redisTime[1]) * 1000) + math.floor(tonumber(redisTime[2]) / 1000)
            local deadline = tonumber(redis.call('HGET', KEYS[1], 'deadlineEpochMillis'))
            if nowMillis < deadline then return {0} end
            local first = redis.call('HGET', KEYS[1], 'candidate:1')
            local second = redis.call('HGET', KEYS[1], 'candidate:2')
            local responded = tonumber(redis.call('HGET', KEYS[1], 'respondedCount') or '0')
            local firstCount = tonumber(redis.call('HGET', KEYS[1], 'count:' .. first) or '0')
            local secondCount = tonumber(redis.call('HGET', KEYS[1], 'count:' .. second) or '0')
            if responded > 0 and firstCount ~= secondCount then
                local winner = firstCount > secondCount and first or second
                redis.call('HSET', KEYS[1], 'status', 'SELECTED', 'selectedCandidateId', winner)
                return {2, winner}
            end
            redis.call('HSET', KEYS[1], 'status', 'OWNER_SELECTION_PENDING')
            return {3}
            """, List.class);

    private static final DefaultRedisScript<Long> OWNER_SELECTABLE_SCRIPT = script(
            """
            if redis.call('HGET', KEYS[1], 'status') ~= 'OWNER_SELECTION_PENDING' then return 0 end
            if redis.call('HGET', KEYS[1], 'candidate:1') == ARGV[1]
                or redis.call('HGET', KEYS[1], 'candidate:2') == ARGV[1] then return 1 end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;
    private final Duration votingDuration;

    /** Redis 저장소와 최종 투표 만료·진행 시간을 설정한다. */
    @Autowired
    public RedisFinalMenuVoteRepository(
            StringRedisTemplate redisTemplate,
            @Value("${gm.vote.redis.final-vote-ttl:PT2H}") Duration ttl,
            @Value("${gm.vote.final-vote-duration:PT30M}") Duration votingDuration
    ) {
        Assert.notNull(redisTemplate, "redisTemplate must not be null");
        Assert.notNull(ttl, "ttl must not be null");
        Assert.notNull(votingDuration, "votingDuration must not be null");
        Assert.isTrue(!ttl.isZero() && !ttl.isNegative(), "ttl must be positive");
        Assert.isTrue(!votingDuration.isZero() && !votingDuration.isNegative(),
                "votingDuration must be positive");
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
        this.votingDuration = votingDuration;
    }

    /** 테스트와 기본 구성에서 1차 투표 시간을 최종 투표 시간으로 사용한다. */
    public RedisFinalMenuVoteRepository(StringRedisTemplate redisTemplate, Duration ttl) {
        this(redisTemplate, ttl, MenuVotePolicy.VOTING_DURATION);
    }

    /** {@inheritDoc} */
    @Override
    public void initialize(UUID voteSessionId, List<UUID> candidateIds, int eligibleVoterCount) {
        Assert.notNull(voteSessionId, "voteSessionId must not be null");
        Assert.isTrue(candidateIds != null && candidateIds.size() == 2,
                "final vote must have exactly two candidates");
        Assert.isTrue(!candidateIds.get(0).equals(candidateIds.get(1)),
                "final vote candidates must be distinct");
        Assert.isTrue(eligibleVoterCount > 0, "eligibleVoterCount must be positive");
        long effectiveTtl = Math.max(ttl.toMillis(), votingDuration.toMillis() + 1_000L);
        redisTemplate.execute(
                INITIALIZE_SCRIPT,
                List.of(VoteRedisKeys.finalSession(voteSessionId), DEADLINE_INDEX),
                candidateIds.get(0).toString(), candidateIds.get(1).toString(),
                String.valueOf(eligibleVoterCount), String.valueOf(votingDuration.toMillis()),
                String.valueOf(effectiveTtl), voteSessionId.toString());
    }

    /** {@inheritDoc} */
    @Override
    public FinalMenuVoteResult submit(UUID voteSessionId, UUID userId, UUID candidateId) {
        List<?> result = redisTemplate.execute(
                SUBMIT_SCRIPT,
                List.of(VoteRedisKeys.finalSession(voteSessionId), DEADLINE_INDEX),
                userId.toString(), candidateId.toString(), voteSessionId.toString());
        int outcome = outcome(result);
        if (outcome == -2) throw new VoteCandidateException(VoteCandidateErrorCode.CANDIDATE_NOT_FOUND);
        if (outcome == -1) throw new VoteCandidateException(VoteCandidateErrorCode.FINAL_VOTE_NOT_ALLOWED);
        if (outcome == 1) return FinalMenuVoteResult.waiting();
        if (outcome == 2) return FinalMenuVoteResult.selected(uuid(result, 1));
        return FinalMenuVoteResult.tied(List.of(uuid(result, 1), uuid(result, 2)));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<FinalMenuVoteState> findState(UUID voteSessionId) {
        Map<Object, Object> values = redisTemplate.opsForHash()
                .entries(VoteRedisKeys.finalSession(voteSessionId));
        if (values.isEmpty()) return Optional.empty();
        UUID first = UUID.fromString(value(values, "candidate:1"));
        UUID second = UUID.fromString(value(values, "candidate:2"));
        String selected = (String) values.get("selectedCandidateId");
        return Optional.of(new FinalMenuVoteState(
                FinalMenuVoteState.Status.valueOf(value(values, "status")),
                Instant.ofEpochMilli(Long.parseLong(value(values, "deadlineEpochMillis"))),
                List.of(
                        new FinalMenuVoteCount(first, integer(values, "count:" + first)),
                        new FinalMenuVoteCount(second, integer(values, "count:" + second))
                ),
                integer(values, "respondedCount"),
                selected == null ? null : UUID.fromString(selected)
        ));
    }

    /** {@inheritDoc} */
    @Override
    public List<UUID> findExpired(Instant now, int limit) {
        if (limit <= 0) return List.of();
        Set<String> members = redisTemplate.opsForZSet().rangeByScore(
                DEADLINE_INDEX, Double.NEGATIVE_INFINITY, now.toEpochMilli(), 0, limit);
        if (members == null || members.isEmpty()) return List.of();
        return members.stream().map(UUID::fromString).toList();
    }

    /** {@inheritDoc} */
    @Override
    public FinalMenuVoteCloseResult closeExpired(UUID voteSessionId) {
        List<?> result = redisTemplate.execute(
                CLOSE_EXPIRED_SCRIPT,
                List.of(VoteRedisKeys.finalSession(voteSessionId)));
        int outcome = outcome(result);
        if (outcome == -1) return FinalMenuVoteCloseResult.notFound();
        if (outcome == 0) return FinalMenuVoteCloseResult.notDue();
        if (outcome == 2) return FinalMenuVoteCloseResult.uniqueWinner(uuid(result, 1));
        return FinalMenuVoteCloseResult.ownerSelectionPending();
    }

    /** {@inheritDoc} */
    @Override
    public void removeExpiration(UUID voteSessionId) {
        redisTemplate.opsForZSet().remove(DEADLINE_INDEX, voteSessionId.toString());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTiedCandidate(UUID voteSessionId, UUID candidateId) {
        Long result = redisTemplate.execute(
                OWNER_SELECTABLE_SCRIPT,
                List.of(VoteRedisKeys.finalSession(voteSessionId)), candidateId.toString());
        return Long.valueOf(1L).equals(result);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(UUID voteSessionId) {
        redisTemplate.delete(VoteRedisKeys.finalSession(voteSessionId));
        removeExpiration(voteSessionId);
    }

    /** Lua 반환 목록의 첫 값을 결과 코드로 변환한다. */
    private static int outcome(List<?> result) {
        return result == null || result.isEmpty() ? -1 : number(result.get(0));
    }

    /** Lua 반환 목록의 값을 UUID로 변환한다. */
    private static UUID uuid(List<?> result, int index) {
        return UUID.fromString(result.get(index).toString());
    }

    /** Redis Hash 숫자 필드를 읽고 값이 없으면 0을 반환한다. */
    private static int integer(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? 0 : Integer.parseInt(value.toString());
    }

    /** 필수 Redis Hash 필드를 문자열로 읽는다. */
    private static String value(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) throw new IllegalStateException("Missing final vote field: " + key);
        return value.toString();
    }

    /** Redis 실행 결과를 정수로 변환한다. */
    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
    }

    /** Lua 원문과 반환 형식으로 실행 스크립트를 만든다. */
    private static <T> DefaultRedisScript<T> script(String source, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setScriptText(source);
        script.setResultType(resultType);
        return script;
    }
}
