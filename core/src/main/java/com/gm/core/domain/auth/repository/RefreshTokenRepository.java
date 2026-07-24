package com.gm.core.domain.auth.repository;

import java.time.Duration;
import java.util.UUID;

/**
 * Refresh Token 저장소 계약이다.
 *
 * <p>구현체는 Redis에 Refresh Token을 저장하며, 원문 토큰을 그대로 저장하지 않고 안전한 해시값으로 저장해야 한다.</p>
 *
 * <p>Refresh Token의 고유 식별자인 refreshTokenId를 저장소 키로 사용하여
 * 하나의 회원이 여러 기기에서 독립적인 로그인 세션을 유지할 수 있도록 한다.</p>
 */
public interface RefreshTokenRepository {

    /**
     * Refresh Token을 저장한다.
     *
     * @param refreshTokenId Refresh Token의 JWT ID(jti)
     * @param userId         Refresh Token 소유 회원 UUID
     * @param refreshToken   저장할 Refresh Token 원문
     * @param expiration     Redis에 적용할 만료 시간
     */
    void save(String refreshTokenId, UUID userId, String refreshToken, Duration expiration);

    /**
     * 요청된 Refresh Token이 Redis에 저장된 현재 기기 세션과 일치하는지 확인한다.
     *
     * <p>구현체에서는 저장된 해시값과 요청 토큰의 해시값을 안전한 방식으로 비교해야 한다.</p>
     *
     * @param refreshTokenId Refresh Token의 JWT ID(jti)
     * @param userId         Refresh Token에서 추출한 회원 UUID
     * @param refreshToken   검증할 Refresh Token 원문
     * @return 회원, 토큰 ID, 토큰 값이 모두 일치하면 {@code true}
     */
    boolean matches(String refreshTokenId, UUID userId, String refreshToken);

    /**
     * 지정한 Refresh Token 세션을 삭제한다.
     *
     * <p>사용자 전체 세션이 아니라 해당 refreshTokenId에 해당하는 현재 기기 세션만 삭제한다.</p>
     *
     * @param refreshTokenId 삭제할 Refresh Token의 JWT ID(jti)
     */
    void delete(String refreshTokenId);
}