package com.gm.api.auth.service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Refresh Token HTTP 쿠키의 생성, 조회 및 삭제를 담당한다.
 */
@Component
public class RefreshTokenCookieManager {

    private final String cookieName;
    private final String cookiePath;
    private final boolean secure;
    private final String sameSite;
    private final Duration refreshTokenExpiration;

    public RefreshTokenCookieManager(
            @Value("${auth.refresh-token-cookie.name:refresh_token}")
            String cookieName,

            @Value("${auth.refresh-token-cookie.path:/api/auth}")
            String cookiePath,

            @Value("${auth.refresh-token-cookie.secure:true}")
            boolean secure,

            @Value("${auth.refresh-token-cookie.same-site:Lax}")
            String sameSite,

            @Value("${jwt.refresh-token-expiration}")
            long refreshTokenExpirationSeconds
    ) {
        validateConfiguration(cookieName, cookiePath, sameSite, refreshTokenExpirationSeconds);

        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.secure = secure;
        this.sameSite = sameSite;
        this.refreshTokenExpiration = Duration.ofSeconds(refreshTokenExpirationSeconds);
    }

    /**
     * Refresh Token을 HTTP-only 쿠키로 응답에 추가한다.
     *
     * @param response     현재 HTTP 응답
     * @param refreshToken 저장할 Refresh Token
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (response == null) { throw new IllegalArgumentException("HTTP 응답은 null일 수 없습니다."); }

        if (!StringUtils.hasText(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token은 null이거나 공백일 수 없습니다.");
        }

        ResponseCookie cookie = createCookie(refreshToken, refreshTokenExpiration);

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 요청 쿠키에서 Refresh Token을 조회한다.
     *
     * @param request 현재 HTTP 요청
     * @return Refresh Token
     */
    public Optional<String> resolveRefreshToken(HttpServletRequest request) {
        if (request == null) { return Optional.empty(); }

        Cookie[] cookies = request.getCookies();

        if (cookies == null || cookies.length == 0) { return Optional.empty(); }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst();
    }

    /**
     * 요청 쿠키에서 Refresh Token을 조회한다.
     * <p>Refresh Token이 존재하지 않으면 {@code null}을 반환한다.</p>
     */
    public String resolveRefreshTokenOrNull(HttpServletRequest request) {
        return resolveRefreshToken(request).orElse(null);
    }

    /**
     * Refresh Token 쿠키를 삭제한다.
     * <p>동일한 이름과 경로로 Max-Age가 0인 쿠키를 응답에 추가한다.</p>
     *
     * @param response 현재 HTTP 응답
     */
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        if (response == null) {throw new IllegalArgumentException("HTTP 응답은 null일 수 없습니다.");}

        ResponseCookie expiredCookie = createCookie("", Duration.ZERO);

        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
    }

    private ResponseCookie createCookie(String value, Duration maxAge) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .path(cookiePath)
                .sameSite(sameSite)
                .maxAge(maxAge)
                .build();
    }

    private void validateConfiguration(
            String cookieName, String cookiePath, String sameSite, long refreshTokenExpirationSeconds) {
        if (!StringUtils.hasText(cookieName)) {
            throw new IllegalArgumentException("Refresh Token 쿠키 이름은 null이거나 공백일 수 없습니다.");
        }
        if (!StringUtils.hasText(cookiePath) || !cookiePath.startsWith("/")) {
            throw new IllegalArgumentException("Refresh Token 쿠키 경로는 '/'로 시작해야 합니다.");
        }
        if (!StringUtils.hasText(sameSite)) {
            throw new IllegalArgumentException("Refresh Token 쿠키 SameSite가 설정되지 않았습니다.");
        }
        if (!isSupportedSameSite(sameSite)) {
            throw new IllegalArgumentException("Refresh Token 쿠키 SameSite는 " + "Strict, Lax, None 중 하나여야 합니다.");
        }
        if (refreshTokenExpirationSeconds <= 0) {
            throw new IllegalArgumentException("Refresh Token 쿠키 만료 시간은 0보다 커야 합니다.");
        }
    }

    private boolean isSupportedSameSite(String sameSite) {
        return "Strict".equalsIgnoreCase(sameSite)
                || "Lax".equalsIgnoreCase(sameSite)
                || "None".equalsIgnoreCase(sameSite);
    }

    public String getCookieName() { return cookieName; }
    public String getCookiePath() { return cookiePath; }
    public boolean isSecure() { return secure; }
    public String getSameSite() { return sameSite; }
}