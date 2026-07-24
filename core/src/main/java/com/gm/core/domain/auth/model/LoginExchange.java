package com.gm.core.domain.auth.model;

import java.util.UUID;

/**
 * OAuth 로그인 성공 후 프런트엔드가 최초 Access Token을 안전하게 조회하기 위해 사용하는 일회성 로그인 교환 정보다.
 *
 * @param userId         OAuth 인증에 성공한 회원 UUID
 * @param refreshTokenId 발급된 Refresh Token의 JWT ID(jti)
 */
public record LoginExchange(UUID userId, String refreshTokenId) {

    /**
     * 일회성 로그인 교환 정보를 생성한다.
     *
     * @param userId         회원 UUID
     * @param refreshTokenId Refresh Token의 JWT ID(jti)
     * @return 생성된 로그인 교환 정보
     */
    public static LoginExchange of(UUID userId, String refreshTokenId) {
        return new LoginExchange(userId, refreshTokenId);
    }
}