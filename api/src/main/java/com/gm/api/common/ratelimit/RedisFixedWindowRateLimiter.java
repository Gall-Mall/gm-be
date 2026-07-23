package com.gm.api.common.ratelimit;

import java.time.Duration;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis {@code INCR}+{@code EXPIRE}로 구현한 고정 윈도우(fixed window) rate limiter다.
 *
 * <p>bucket4j 같은 별도 라이브러리 없이, 키 하나당 하나의 카운터로 "윈도우 시작 시점부터
 * 지금까지 몇 번 호출됐는가"만 센다. 완벽한 슬라이딩 윈도우는 아니지만 초대 도메인처럼
 * 대략적인 남용 방지가 목적인 경우엔 충분하다.</p>
 */
@Component
@RequiredArgsConstructor
public class RedisFixedWindowRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * 주어진 키의 호출 횟수를 하나 증가시키고, 윈도우 한도 이내인지 확인한다.
     *
     * <p>카운터가 처음 생성된 시점(값이 1)에만 TTL을 설정해, 이후 같은 윈도우 안의 호출들이
     * TTL을 계속 늘리지 않도록 한다.</p>
     *
     * @param key rate limit을 적용할 대상을 식별하는 Redis 키
     * @param limit 윈도우 안에서 허용할 최대 호출 횟수
     * @param window 카운터가 리셋되는 주기
     * @return 이번 호출까지 포함해 한도 이내면 {@code true}
     */
    public boolean tryConsume(String key, int limit, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count != null && count <= limit;
    }
}
