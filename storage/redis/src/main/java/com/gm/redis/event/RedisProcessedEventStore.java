package com.gm.redis.event;

import java.time.Duration;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.gm.core.event.ProcessedEventStore;

/**
 * SET NX EX 기반 dedup. DB 트랜잭션이 없는 경로(화면 push, 외부 알림)에 쓴다.
 * EXISTS 후 SET으로 나누면 그 틈에 다른 컨슈머가 끼어들 수 있어 setIfAbsent를 쓴다.
 */
@Component("redisProcessedEventStore")
@RequiredArgsConstructor
public class RedisProcessedEventStore implements ProcessedEventStore {

    private static final String KEY_PREFIX = "mq:evt:";

    /** 재전달은 수 분 내에 끝나지만 DLQ 수동 재처리를 감안해 하루로 잡는다. */
    private static final Duration TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean markIfFirst(String eventId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + eventId, "1", TTL));
    }

    @Override
    public void release(String eventId) {
        redisTemplate.delete(KEY_PREFIX + eventId);
    }
}
