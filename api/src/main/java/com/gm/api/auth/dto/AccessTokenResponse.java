package com.gm.api.auth.dto;

import org.springframework.util.StringUtils;

/**
 * Access Token 발급 응답이다.
 * <p>OAuth 로그인 교환 및 Access Token 재발급 시 사용한다. Refresh Token은 HTTP-only Cookie로 전달되므로 응답 본문에 포함하지 않는다.</p>
 *
 * @param tokenType Bearer
 * @param accessToken 발급된 Access Token
 * @param expiresIn Access Token 만료 시간(초)
 */
public record AccessTokenResponse(String tokenType, String accessToken, long expiresIn) {

    private static final String TOKEN_TYPE = "Bearer";

    /**
     * Access Token 응답을 생성한다.
     *
     * @param accessToken 발급된 Access Token
     * @param expiresIn Access Token 만료 시간(초)
     * @return Access Token 응답
     */
    public static AccessTokenResponse of(String accessToken, long expiresIn) {
        return new AccessTokenResponse(TOKEN_TYPE, accessToken, expiresIn);
    }

    /**
     * Record 생성 시 값을 검증한다.
     */
    public AccessTokenResponse {

        if (!StringUtils.hasText(tokenType)) {
            throw new IllegalArgumentException("Token Type은 null이거나 공백일 수 없습니다.");
        }
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("Access Token은 null이거나 공백일 수 없습니다.");
        }
        if (expiresIn <= 0) { throw new IllegalArgumentException("만료 시간은 0보다 커야 합니다."); }
    }
}