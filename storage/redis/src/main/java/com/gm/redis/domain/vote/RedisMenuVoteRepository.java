package com.gm.redis.domain.vote;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteChoice;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteCloseResult;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteSession;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteState;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteSubmission;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteSubmitResult;
import com.gm.core.domain.vote.candidate.repository.menuvote.MenuVoteRepository;

/**
 * 메뉴 투표의 불변 메타데이터, 사용자 선택, 후보별 집계를 세션별 단일 Redis Hash에 저장한다.
 * Redis 서버 시간과 Lua Script를 사용해 초기화, 선택 변경, 집계, 마감을 원자적으로 처리한다.
 */
@Repository
public class RedisMenuVoteRepository implements MenuVoteRepository {

    private static final long DEADLINE_TTL_MARGIN_MILLIS = 1_000L;
    private static final int COUNT_VALUES_PER_CANDIDATE = 5;

    /** 최초 초기화만 메타데이터를 기록하고 Redis TIME으로 마감 시각을 계산한다. */
    private static final DefaultRedisScript<Long> INITIALIZE_SCRIPT = script(
            """
            if redis.call('EXISTS', KEYS[1]) == 1 then
                return 0
            end

            local redisTime = redis.call('TIME')
            local nowMillis = (tonumber(redisTime[1]) * 1000) + math.floor(tonumber(redisTime[2]) / 1000)
            local deadlineMillis = nowMillis + tonumber(ARGV[3])
            redis.call('HSET', KEYS[1],
                'sessionId', ARGV[1],
                'status', 'OPEN',
                'candidateIds', ARGV[2],
                'deadlineEpochMillis', tostring(deadlineMillis))
            for index = 5, #ARGV do
                redis.call('HSET', KEYS[1], 'candidate:' .. ARGV[index], '1')
            end
            redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[4]))
            return 1
            """,
            Long.class
    );

    /**
     * 소속 후보와 Redis 서버 기준 마감 시각을 검증한 뒤 선택 및 집계를 함께 변경한다.
     * 반환 첫 값은 성공 1, 종료/미초기화 0, 다른 세션 후보 -1을 뜻한다.
     */
    private static final DefaultRedisScript<List> SUBMIT_SCRIPT = script(
            """
            if redis.call('HGET', KEYS[1], 'status') ~= 'OPEN' then
                return {0}
            end

            local redisTime = redis.call('TIME')
            local nowMillis = (tonumber(redisTime[1]) * 1000) + math.floor(tonumber(redisTime[2]) / 1000)
            local deadlineMillis = tonumber(redis.call('HGET', KEYS[1], 'deadlineEpochMillis'))
            if not deadlineMillis or nowMillis >= deadlineMillis then
                redis.call('HSET', KEYS[1], 'status', 'CLOSED')
                redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[4]))
                return {0}
            end
            if redis.call('HEXISTS', KEYS[1], 'candidate:' .. ARGV[1]) == 0 then
                return {-1}
            end

            local candidateId = ARGV[1]
            local choiceField = 'vote:' .. candidateId .. ':' .. ARGV[2]
            local newChoice = ARGV[3]
            local countPrefix = 'count:' .. candidateId .. ':'
            local oldChoice = redis.call('HGET', KEYS[1], choiceField)
            local changed = 0
            if oldChoice ~= newChoice then
                if oldChoice then
                    redis.call('HINCRBY', KEYS[1], countPrefix .. oldChoice, -1)
                else
                    redis.call('HINCRBY', KEYS[1], countPrefix .. 'RESPONDENTS', 1)
                end
                redis.call('HINCRBY', KEYS[1], countPrefix .. newChoice, 1)
                redis.call('HSET', KEYS[1], choiceField, newChoice)
                changed = 1
            end

            local goCount = tonumber(redis.call('HGET', KEYS[1], countPrefix .. 'GO') or '0')
            local maybeCount = tonumber(redis.call('HGET', KEYS[1], countPrefix .. 'MAYBE') or '0')
            local noCount = tonumber(redis.call('HGET', KEYS[1], countPrefix .. 'NO') or '0')
            local respondentCount = tonumber(redis.call('HGET', KEYS[1], countPrefix .. 'RESPONDENTS') or '0')
            return {1, changed, goCount, maybeCount, noCount, respondentCount}
            """,
            List.class
    );

    /** 세션을 닫고 저장된 후보 순서와 집계를 한 원자 연산의 결과로 반환한다. */
    private static final DefaultRedisScript<List> CLOSE_SCRIPT = script(
            """
            local status = redis.call('HGET', KEYS[1], 'status')
            if status ~= 'OPEN' and status ~= 'CLOSED' then
                return {0}
            end
            if status == 'OPEN' then
                if ARGV[2] == '1' then
                    local respondentSum = 0
                    local openCandidateIds = redis.call('HGET', KEYS[1], 'candidateIds')
                    for candidateId in string.gmatch(openCandidateIds, '([^,]+)') do
                        respondentSum = respondentSum + tonumber(redis.call(
                            'HGET', KEYS[1], 'count:' .. candidateId .. ':RESPONDENTS') or '0')
                    end
                    if respondentSum == 0 then
                        return {-2}
                    end
                end
                redis.call('HSET', KEYS[1], 'status', 'CLOSED')
            end
            redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[1]))

            local result = {1}
            local candidateIds = redis.call('HGET', KEYS[1], 'candidateIds')
            for candidateId in string.gmatch(candidateIds, '([^,]+)') do
                local countPrefix = 'count:' .. candidateId .. ':'
                table.insert(result, candidateId)
                table.insert(result, tonumber(redis.call('HGET', KEYS[1], countPrefix .. 'GO') or '0'))
                table.insert(result, tonumber(redis.call('HGET', KEYS[1], countPrefix .. 'MAYBE') or '0'))
                table.insert(result, tonumber(redis.call('HGET', KEYS[1], countPrefix .. 'NO') or '0'))
                table.insert(result, tonumber(redis.call('HGET', KEYS[1], countPrefix .. 'RESPONDENTS') or '0'))
            end
            return result
            """,
            List.class
    );

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;
    private final Duration closedRecoveryTtl;

    /** Redis 연결과 OPEN/CLOSED 투표 데이터의 보관 시간을 설정한다. */
    public RedisMenuVoteRepository(
            StringRedisTemplate redisTemplate,
            @Value("${gm.vote.redis.ttl:PT2H}") Duration ttl,
            @Value("${gm.vote.redis.closed-recovery-ttl:PT24H}") Duration closedRecoveryTtl
    ) {
        Assert.notNull(redisTemplate, "redisTemplate must not be null");
        Assert.notNull(ttl, "ttl must not be null");
        Assert.notNull(closedRecoveryTtl, "closedRecoveryTtl must not be null");
        Assert.isTrue(!ttl.isZero() && !ttl.isNegative(), "ttl must be positive");
        Assert.isTrue(
                !closedRecoveryTtl.isZero() && !closedRecoveryTtl.isNegative(),
                "closedRecoveryTtl must be positive"
        );
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
        this.closedRecoveryTtl = closedRecoveryTtl;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize(MenuVoteSession session) {
        String candidateIds = String.join(",", session.candidateIds().stream().map(UUID::toString).toList());
        long effectiveTtlMillis = Math.max(
                ttl.toMillis(),
                Math.addExact(session.votingDuration().toMillis(), DEADLINE_TTL_MARGIN_MILLIS)
        );
        List<String> arguments = new ArrayList<>();
        arguments.add(session.voteSessionId().toString());
        arguments.add(candidateIds);
        arguments.add(String.valueOf(session.votingDuration().toMillis()));
        arguments.add(String.valueOf(effectiveTtlMillis));
        arguments.addAll(session.candidateIds().stream().map(UUID::toString).toList());

        redisTemplate.execute(
                INITIALIZE_SCRIPT,
                List.of(VoteRedisKeys.session(session.voteSessionId())),
                (Object[]) arguments.toArray(String[]::new)
        );
    }

    /** {@inheritDoc} */
    @Override
    public MenuVoteSubmitResult submit(
            UUID voteSessionId,
            UUID candidateId,
            UUID userId,
            MenuVoteChoice choice
    ) {
        List<?> result = redisTemplate.execute(
                SUBMIT_SCRIPT,
                List.of(VoteRedisKeys.session(voteSessionId)),
                candidateId.toString(),
                userId.toString(),
                choice.name(),
                String.valueOf(closedRecoveryTtl.toMillis())
        );
        int outcome = result == null || result.isEmpty() ? 0 : number(result, 0);
        if (outcome == -1) {
            return MenuVoteSubmitResult.candidateNotFound();
        }
        if (outcome != 1) {
            return MenuVoteSubmitResult.voteClosed();
        }

        return MenuVoteSubmitResult.success(new MenuVoteSubmission(
                choice,
                new MenuVoteCount(
                        candidateId,
                        number(result, 2),
                        number(result, 3),
                        number(result, 4),
                        number(result, 5)
                ),
                number(result, 1) == 1
        ));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<MenuVoteState> findState(UUID voteSessionId) {
        Map<Object, Object> values = redisTemplate.opsForHash()
                .entries(VoteRedisKeys.session(voteSessionId));
        if (values.isEmpty()) {
            return Optional.empty();
        }
        List<UUID> candidateIds = List.of(values.get("candidateIds").toString().split(","))
                .stream()
                .filter(candidateId -> !candidateId.isBlank())
                .map(UUID::fromString)
                .toList();
        List<MenuVoteCount> counts = candidateIds.stream()
                .map(candidateId -> new MenuVoteCount(
                        candidateId,
                        fieldNumber(values, "count:" + candidateId + ":GO"),
                        fieldNumber(values, "count:" + candidateId + ":MAYBE"),
                        fieldNumber(values, "count:" + candidateId + ":NO"),
                        fieldNumber(values, "count:" + candidateId + ":RESPONDENTS")
                ))
                .toList();
        Map<UUID, Integer> responseCountsByUser = new HashMap<>();
        values.keySet().stream()
                .map(Object::toString)
                .filter(field -> field.startsWith("vote:"))
                .map(field -> field.split(":", 3))
                .filter(parts -> parts.length == 3)
                .map(parts -> UUID.fromString(parts[2]))
                .forEach(userId -> responseCountsByUser.merge(userId, 1, Integer::sum));
        List<UUID> completedUserIds = responseCountsByUser.entrySet().stream()
                .filter(entry -> entry.getValue() == candidateIds.size())
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        return Optional.of(new MenuVoteState(
                MenuVoteState.Status.valueOf(values.get("status").toString()),
                Instant.ofEpochMilli(Long.parseLong(values.get("deadlineEpochMillis").toString())),
                counts,
                completedUserIds
        ));
    }

    /** {@inheritDoc} */
    @Override
    public MenuVoteCloseResult closeAndGetSnapshot(UUID voteSessionId) {
        return closeAndGetSnapshot(voteSessionId, false);
    }

    /** {@inheritDoc} */
    @Override
    public MenuVoteCloseResult closeAndGetSnapshotIfAnyResponse(UUID voteSessionId) {
        return closeAndGetSnapshot(voteSessionId, true);
    }

    /** 응답자 조건을 적용해 투표를 닫고 고정된 집계를 반환한다. */
    private MenuVoteCloseResult closeAndGetSnapshot(UUID voteSessionId, boolean requireAnyResponse) {
        List<?> result = redisTemplate.execute(
                CLOSE_SCRIPT,
                List.of(VoteRedisKeys.session(voteSessionId)),
                String.valueOf(closedRecoveryTtl.toMillis()),
                requireAnyResponse ? "1" : "0"
        );
        int outcome = result == null || result.isEmpty() ? 0 : number(result, 0);
        if (outcome == -2) {
            return MenuVoteCloseResult.noResponse();
        }
        if (outcome != 1) {
            return MenuVoteCloseResult.snapshotNotFound();
        }

        List<MenuVoteCount> snapshot = new ArrayList<>();
        for (int index = 1; index < result.size(); index += COUNT_VALUES_PER_CANDIDATE) {
            snapshot.add(new MenuVoteCount(
                    UUID.fromString(result.get(index).toString()),
                    number(result, index + 1),
                    number(result, index + 2),
                    number(result, index + 3),
                    number(result, index + 4)
            ));
        }
        return MenuVoteCloseResult.success(snapshot);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(UUID voteSessionId) {
        redisTemplate.delete(VoteRedisKeys.session(voteSessionId));
    }

    /** Lua Script가 반환한 숫자를 Java 집계 값으로 변환한다. */
    private static int number(List<?> result, int index) {
        Object value = result.get(index);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    /** Hash 집계 필드가 아직 생성되지 않은 후보는 0표로 변환한다. */
    private static int fieldNumber(Map<Object, Object> values, String field) {
        Object value = values.get(field);
        return value == null ? 0 : Integer.parseInt(value.toString());
    }

    /** Script 본문과 반환 타입을 함께 설정한 Redis Script를 생성한다. */
    private static <T> DefaultRedisScript<T> script(String scriptText, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setScriptText(scriptText);
        script.setResultType(resultType);
        return script;
    }
}
