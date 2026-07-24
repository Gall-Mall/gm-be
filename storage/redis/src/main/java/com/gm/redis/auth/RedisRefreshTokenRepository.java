package com.gm.redis.auth;

import java.time.Duration;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.gm.core.domain.auth.repository.RefreshTokenRepository;

/**
 * Redis 기반 Refresh Token 저장소 구현체다.
 * <p>Refresh Token의 JWT ID인 {@code refreshTokenId}를 Redis 키로 사용하여 동일 사용자의 여러 기기 로그인 세션을 독립적으로 관리한다.</p>
 * <p>Refresh Token 원문은 저장하지 않고 SHA-256 해시값만 저장한다.</p>
 */
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String KEY_PREFIX = "auth:refresh-token:";
    private static final String VALUE_DELIMITER = ":";
    private final StringRedisTemplate redisTemplate;

    /**
     * Refresh Token 세션을 Redis에 저장한다.
     *
     * @param refreshTokenId Refresh Token의 JWT ID
     * @param userId         Refresh Token 소유 회원 UUID
     * @param refreshToken   Refresh Token 원문
     * @param expiration     Redis TTL
     */
    @Override
    public void save(String refreshTokenId, UUID userId, String refreshToken, Duration expiration) {
        validateSaveArguments(refreshTokenId, userId, refreshToken, expiration);

        String key = generateKey(refreshTokenId);
        String tokenHash = TokenHashUtils.hash(refreshToken);
        String value = generateValue(userId, tokenHash);

        redisTemplate.opsForValue().set(key, value, expiration);
    }

    /**
     * 요청된 Refresh Token이 Redis에 저장된 세션 정보와 일치하는지 확인한다.
     * <p>다음 세 가지 조건이 모두 만족해야 한다.</p>
     * <ol>
     *     <li>refreshTokenId에 해당하는 Redis 데이터가 존재한다.</li>
     *     <li>Redis에 저장된 userId와 토큰의 userId가 일치한다.</li>
     *     <li>요청 Refresh Token의 해시가 저장된 해시값과 일치한다.</li>
     * </ol>
     *
     * @param refreshTokenId Refresh Token의 JWT ID
     * @param userId         Refresh Token에서 추출한 회원 UUID
     * @param refreshToken   검증할 Refresh Token 원문
     * @return 정상적인 현재 기기 세션이면 {@code true}
     */
    @Override
    public boolean matches(String refreshTokenId, UUID userId, String refreshToken) {
        if (refreshTokenId == null || refreshTokenId.isBlank() || userId == null
                || refreshToken == null || refreshToken.isBlank()) { return false; }

        String storedValue = redisTemplate.opsForValue().get(generateKey(refreshTokenId));

        if (storedValue == null || storedValue.isBlank()) { return false; }

        RefreshTokenValue refreshTokenValue = parseValue(storedValue);

        if (refreshTokenValue == null) { return false; }
        if (!userId.equals(refreshTokenValue.userId())) { return false; }

        return TokenHashUtils.matches(refreshToken, refreshTokenValue.tokenHash());
    }

    /**
     * 지정한 Refresh Token 세션만 Redis에서 삭제한다.
     *
     * @param refreshTokenId 삭제할 Refresh Token의 JWT ID
     */
    @Override
    public void delete(String refreshTokenId) {
        if (refreshTokenId == null || refreshTokenId.isBlank()) { return; }

        redisTemplate.delete(generateKey(refreshTokenId));
    }

    private String generateKey(String refreshTokenId) { return KEY_PREFIX + refreshTokenId; }
    private String generateValue(UUID userId, String tokenHash) { return userId + VALUE_DELIMITER + tokenHash; }

    /**
     * Redis에 저장된 문자열을 회원 UUID와 토큰 해시값으로 분리한다.
     * <p>잘못된 값이 저장되어 있으면 예외를 외부로 전파하지 않고 검증 실패로 처리할 수 있도록 {@code null}을 반환한다.</p>
     */
    private RefreshTokenValue parseValue(String storedValue) {
        int delimiterIndex = storedValue.indexOf(VALUE_DELIMITER);
        if (delimiterIndex <= 0 || delimiterIndex == storedValue.length() - 1) { return null; }

        String userIdValue = storedValue.substring(0, delimiterIndex);
        String tokenHash = storedValue.substring(delimiterIndex + 1);

        try {
            UUID userId = UUID.fromString(userIdValue);

            return new RefreshTokenValue(userId, tokenHash);
        } catch (IllegalArgumentException exception) { return null; }
    }

    private void validateSaveArguments(
            String refreshTokenId, UUID userId, String refreshToken, Duration expiration
    ) {
        if (refreshTokenId == null || refreshTokenId.isBlank()) {
            throw new IllegalArgumentException("Refresh Token ID는 null이거나 공백일 수 없습니다.");
        }

        if (userId == null) { throw new IllegalArgumentException("회원 ID는 null일 수 없습니다."); }

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh Token은 null이거나 공백일 수 없습니다.");
        }

        validateExpiration(expiration);
    }

    private void validateExpiration(Duration expiration) {
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("Redis 만료 시간은 0보다 커야 합니다.");
        }
    }

    /**
     * Redis에 저장된 Refresh Token 세션 값을 나타낸다.
     *
     * @param userId    Refresh Token 소유 회원 UUID
     * @param tokenHash Refresh Token의 SHA-256 해시값
     */
    private record RefreshTokenValue(UUID userId, String tokenHash) {}
}