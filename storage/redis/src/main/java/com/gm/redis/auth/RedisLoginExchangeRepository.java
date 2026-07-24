package com.gm.redis.auth;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.gm.core.domain.auth.model.LoginExchange;
import com.gm.core.domain.auth.repository.LoginExchangeRepository;

/**
 * Redis 기반 일회성 로그인 교환 정보 저장소 구현체다.
 * <p>OAuth 인증 성공 후 생성된 일회성 교환 코드와 회원 정보를 짧은 시간 동안 Redis에 저장한다.</p>
 * <p>교환 정보는 조회와 동시에 삭제되어 같은 코드를 다시 사용할 수 없다.</p>
 */
@Repository
@RequiredArgsConstructor
public class RedisLoginExchangeRepository implements LoginExchangeRepository {

    private static final String KEY_PREFIX = "auth:login-exchange:";
    private static final String VALUE_DELIMITER = ":";
    private final StringRedisTemplate redisTemplate;

    /**
     * 일회성 로그인 교환 정보를 Redis에 저장한다.
     *
     * @param exchangeCode  일회성 로그인 교환 코드
     * @param loginExchange OAuth 로그인 성공 정보
     * @param expiration    교환 정보의 Redis TTL
     */
    @Override
    public void save(String exchangeCode, LoginExchange loginExchange, Duration expiration) {
        validateSaveArguments(exchangeCode, loginExchange, expiration);

        String key = generateKey(exchangeCode);
        String value = serialize(loginExchange);

        redisTemplate.opsForValue().set(key, value, expiration);
    }

    /**
     * 일회성 로그인 교환 정보를 조회하면서 즉시 삭제한다.
     * <p>{@code getAndDelete()}를 사용하므로 정상적으로 조회된 코드는 첫 번째 요청 이후 Redis에서 제거된다.</p>
     *
     * @param exchangeCode 사용할 일회성 로그인 교환 코드
     * @return 유효한 로그인 교환 정보
     */
    @Override
    public Optional<LoginExchange> consume(String exchangeCode) {
        if (exchangeCode == null || exchangeCode.isBlank()) { return Optional.empty(); }

        String storedValue = redisTemplate.opsForValue().getAndDelete(generateKey(exchangeCode));

        if (storedValue == null || storedValue.isBlank()) { return Optional.empty(); }

        return deserialize(storedValue);
    }

    private String generateKey(String exchangeCode) { return KEY_PREFIX + exchangeCode; }

    private String serialize(LoginExchange loginExchange) {
        return loginExchange.userId() + VALUE_DELIMITER + loginExchange.refreshTokenId();
    }

    private Optional<LoginExchange> deserialize(String storedValue) {
        int delimiterIndex = storedValue.indexOf(VALUE_DELIMITER);

        if (delimiterIndex <= 0 || delimiterIndex == storedValue.length() - 1) { return Optional.empty(); }

        String userIdValue = storedValue.substring(0, delimiterIndex);
        String refreshTokenId = storedValue.substring(delimiterIndex + 1);

        if (refreshTokenId.isBlank()) { return Optional.empty(); }

        try {
            UUID userId = UUID.fromString(userIdValue);

            return Optional.of(LoginExchange.of(userId, refreshTokenId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private void validateSaveArguments(String exchangeCode, LoginExchange loginExchange, Duration expiration) {
        if (exchangeCode == null || exchangeCode.isBlank()) {
            throw new IllegalArgumentException("로그인 교환 코드는 null이거나 공백일 수 없습니다.");
        }

        if (loginExchange == null) {
            throw new IllegalArgumentException("로그인 교환 정보는 null일 수 없습니다.");
        }

        if (loginExchange.userId() == null) {
            throw new IllegalArgumentException("로그인 교환 정보의 회원 ID는 null일 수 없습니다.");
        }

        if (loginExchange.refreshTokenId() == null || loginExchange.refreshTokenId().isBlank()) {
            throw new IllegalArgumentException("로그인 교환 정보의 Refresh Token ID는 " + "null이거나 공백일 수 없습니다.");
        }

        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("로그인 교환 정보의 만료 시간은 0보다 커야 합니다.");
        }
    }
}