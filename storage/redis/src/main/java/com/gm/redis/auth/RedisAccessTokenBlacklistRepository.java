package com.gm.redis.auth;

import java.time.Duration;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.gm.core.domain.auth.repository.AccessTokenBlacklistRepository;

/**
 * Redis 기반 Access Token 블랙리스트 저장소 구현체다.
 * <p>로그아웃된 Access Token의 JWT ID(jti)를 Redis에 저장한다. 저장 데이터의 TTL은 해당 Access Token의 남은 유효시간으로 설정한다.</p>
 */
@Repository
@RequiredArgsConstructor
public class RedisAccessTokenBlacklistRepository implements AccessTokenBlacklistRepository {

    private static final String KEY_PREFIX = "auth:blacklist:access:";
    private static final String BLACKLIST_VALUE = "logout";
    private final StringRedisTemplate redisTemplate;

    /**
     * Access Token ID를 블랙리스트에 등록한다.
     *
     * @param accessTokenId Access Token의 JWT ID
     * @param expiration    Access Token의 남은 유효시간
     */
    @Override
    public void save(String accessTokenId, Duration expiration) {
        validateSaveArguments(accessTokenId, expiration);

        redisTemplate.opsForValue().set(generateKey(accessTokenId), BLACKLIST_VALUE, expiration);
    }

    /**
     * Access Token ID가 블랙리스트에 등록되어 있는지 확인한다.
     *
     * @param accessTokenId 확인할 Access Token의 JWT ID
     * @return 블랙리스트에 존재하면 {@code true}
     */
    @Override
    public boolean exists(String accessTokenId) {
        if (accessTokenId == null || accessTokenId.isBlank()) { return false; }

        Boolean exists = redisTemplate.hasKey(generateKey(accessTokenId));

        return Boolean.TRUE.equals(exists);
    }

    private String generateKey(String accessTokenId) { return KEY_PREFIX + accessTokenId; }

    private void validateSaveArguments(String accessTokenId, Duration expiration) {
        if (accessTokenId == null || accessTokenId.isBlank()) {
            throw new IllegalArgumentException("Access Token ID는 null이거나 공백일 수 없습니다.");
        }

        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("블랙리스트 만료 시간은 0보다 커야 합니다.");
        }
    }
}