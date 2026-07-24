package com.gm.api.security.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.gm.core.domain.user.model.UserStatus;

/**
 * 서비스 JWT의 생성, 검증 및 Claim 추출을 담당한다.
 * <p>Access Token과 Refresh Token은 동일한 서명 키를 사용하지만, {@code tokenType} Claim으로 용도를 구분한다.</p>
 */

@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String STATUS_CLAIM = "status";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    /**
     * JWT Provider를 생성한다.
     *
     * @param secret                 Base64로 인코딩된 JWT 서명 키
     * @param accessTokenExpiration  Access Token 만료 시간(초)
     * @param refreshTokenExpiration Refresh Token 만료 시간(초)
     */
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        validateConfiguration(secret, accessTokenExpiration, refreshTokenExpiration);

        this.key = createSecretKey(secret);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * 서비스 Access Token을 생성한다.
     * <p>Access Token에는 인가 판단에 필요한 회원 상태를 포함한다.</p>
     *
     * @param userId 서비스 회원 UUID
     * @param status 사용자 상태
     * @return 생성된 Access Token
     */
    public String createAccessToken(UUID userId, UserStatus status) {
        validateUserId(userId);
        validateUserStatus(status);

        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plusSeconds(accessTokenExpiration);

        return Jwts.builder()
                // JWT 고유 식별자(jti)
                .id(UUID.randomUUID().toString())
                // 서비스 회원 UUID(sub)
                .subject(userId.toString())
                // Access Token 타입
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                // 사용자 상태
                .claim(STATUS_CLAIM, status.name())
                // 발급 시각
                .issuedAt(Date.from(issuedAt))
                // 만료 시각
                .expiration(Date.from(expiration))
                // 서명
                .signWith(key).compact();
    }

    /**
     * 서비스 Refresh Token을 생성한다.
     * <p>Refresh Token에는 회원 UUID와 토큰 종류만 포함한다. 회원 상태는 Access Token을 재발급할 때 DB에서 다시 조회한다.</p>
     * <p>매 로그인 세션마다 서로 다른 jti가 생성되므로, 여러 기기의 Refresh Token을 독립적으로 관리할 수 있다.</p>
     *
     * @param userId 서비스 회원 UUID
     * @return 생성된 Refresh Token
     */
    public String createRefreshToken(UUID userId) {
        validateUserId(userId);

        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plusSeconds(refreshTokenExpiration);

        return Jwts.builder()
                // 현재 기기 로그인 세션의 고유 식별자
                .id(UUID.randomUUID().toString())
                // 서비스 회원 UUID
                .subject(userId.toString())
                // 토큰 종류
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                // 발급 시각
                .issuedAt(Date.from(issuedAt))
                // 만료 시각
                .expiration(Date.from(expiration))
                // JWT 서명
                .signWith(key).compact();
    }

    /**
     * Access Token을 검증하고 Claims를 반환한다.
     * <p>검증 항목은 다음과 같다.</p>
     * <ul>
     *     <li>토큰 존재 여부</li>
     *     <li>JWT 형식</li>
     *     <li>서명 유효성</li>
     *     <li>만료 여부</li>
     *     <li>Access Token 타입 여부</li>
     * </ul>
     *
     * @param token 검증할 Access Token
     * @return 검증된 JWT Claims
     * @throws IllegalArgumentException 토큰이 없으면 발생
     * @throws JwtException             JWT가 유효하지 않으면 발생
     */
    public Claims validateAccessToken(String token) {
        return validateTokenType(token, ACCESS_TOKEN_TYPE, "Access Token");
    }

    /**
     * Refresh Token을 검증하고 Claims를 반환한다.
     * <p>검증 항목은 다음과 같다.</p>
     * <ul>
     *     <li>토큰 존재 여부</li>
     *     <li>JWT 형식</li>
     *     <li>서명 유효성</li>
     *     <li>만료 여부</li>
     *     <li>Refresh Token 타입 여부</li>
     * </ul>
     *
     * @param token 검증할 Refresh Token
     * @return 검증된 JWT Claims
     * @throws IllegalArgumentException 토큰이 없으면 발생
     * @throws JwtException             JWT가 유효하지 않으면 발생
     */
    public Claims validateRefreshToken(String token) {
        return validateTokenType(token, REFRESH_TOKEN_TYPE, "Refresh Token");
    }

    /**
     * 기존 코드와의 호환성을 위해 유지하는 Access Token 검증 메서드다.
     *
     * @param token Access Token
     * @return 검증된 JWT Claims
     */
    public Claims validate(String token) { return validateAccessToken(token); }

    /**
     * Claims에서 회원 UUID를 반환한다.
     *
     * @param claims JWT Claims
     * @return 회원 UUID
     */
    public UUID getUserId(Claims claims) {

        validateClaims(claims);
        String subject = claims.getSubject();

        if (!StringUtils.hasText(subject)) { throw new JwtException("회원 식별자가 존재하지 않습니다."); }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new JwtException("회원 식별자 형식이 올바르지 않습니다.", exception);
        }
    }

    /**
     * Claims에서 회원 상태를 반환한다.
     * <p>회원 상태는 Access Token에만 포함된다.</p>
     *
     * @param claims Access Token Claims
     * @return 회원 상태
     */
    public UserStatus getUserStatus(Claims claims) {

        validateClaims(claims);
        String status = claims.get(STATUS_CLAIM, String.class);

        if (!StringUtils.hasText(status)) { throw new JwtException("회원 상태가 존재하지 않습니다."); }

        try {
            return UserStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new JwtException("회원 상태가 올바르지 않습니다.", exception);
        }
    }

    /**
     * Claims에서 JWT 고유 식별자(jti)를 반환한다.
     *
     * @param claims JWT Claims
     * @return JWT ID
     */
    public String getJti(Claims claims) {

        validateClaims(claims);
        String jti = claims.getId();

        if (!StringUtils.hasText(jti)) { throw new JwtException("JWT ID가 존재하지 않습니다."); }

        return jti;
    }

    /**
     * Claims에서 토큰 만료 시각을 반환한다.
     *
     * @param claims JWT Claims
     * @return 토큰 만료 시각
     */
    public Instant getExpiration(Claims claims) {

        validateClaims(claims);
        Date expiration = claims.getExpiration();

        if (expiration == null) { throw new JwtException("토큰 만료 시간이 존재하지 않습니다.");}

        return expiration.toInstant();
    }

    /**
     * 토큰의 남은 유효시간을 반환한다.
     * <p>로그아웃 시 Access Token을 Redis 블랙리스트에 저장할 때 Redis TTL로 사용한다.</p>
     *
     * @param claims JWT Claims
     * @return 현재 시각부터 만료 시각까지 남은 시간
     * @throws JwtException 이미 만료되었거나 남은 시간이 없으면 발생
     */
    public Duration getRemainingExpiration(Claims claims) {
        Instant expiration = getExpiration(claims);
        Instant now = Instant.now();

        Duration remainingExpiration = Duration.between(
                now,
                expiration
        );

        if (remainingExpiration.isZero()
                || remainingExpiration.isNegative()) {
            throw new JwtException("이미 만료된 토큰입니다.");
        }

        return remainingExpiration;
    }

    /**
     * Access Token 만료 시간을 초 단위로 반환한다.
     *
     * @return Access Token 만료 시간(초)
     */
    public long getAccessTokenExpirationSeconds() { return accessTokenExpiration; }

    /**
     * Access Token 만료 시간을 Duration으로 반환한다.
     *
     * @return Access Token 만료 시간
     */
    public Duration getAccessTokenExpiration() { return Duration.ofSeconds(accessTokenExpiration); }

    /**
     * Refresh Token 만료 시간을 초 단위로 반환한다.
     *
     * @return Refresh Token 만료 시간(초)
     */
    public long getRefreshTokenExpirationSeconds() { return refreshTokenExpiration; }

    /**
     * Refresh Token 만료 시간을 Duration으로 반환한다.
     * <p>Refresh Token을 Redis에 저장할 때 TTL로 사용한다.</p>
     *
     * @return Refresh Token 만료 시간
     */
    public Duration getRefreshTokenExpiration() { return Duration.ofSeconds(refreshTokenExpiration); }

    /**
     * 토큰을 파싱한 뒤 기대한 토큰 종류인지 검증한다.
     */
    private Claims validateTokenType(String token, String expectedTokenType, String tokenName) {
        if (!StringUtils.hasText(token)) { throw new IllegalArgumentException(tokenName + "이 존재하지 않습니다."); }

        Claims claims = parseClaims(token);
        String actualTokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        if (!expectedTokenType.equals(actualTokenType)) { throw new JwtException(tokenName + " 타입이 올바르지 않습니다."); }

        return claims;
    }

    /**
     * JWT의 서명과 만료 시간을 검증하고 Claims를 반환한다.
     * <p>서명이 올바르지 않거나 토큰이 만료된 경우, JJWT에서 {@link JwtException} 계열 예외를 발생시킨다.</p>
     */
    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /**
     * Base64 문자열로부터 JWT 서명 키를 생성한다.
     */
    private SecretKey createSecretKey(String secret) {
        try {
            byte[] decodedSecret = Base64.getDecoder().decode(secret);

            return Keys.hmacShaKeyFor(decodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("JWT Secret은 유효한 Base64 형식이어야 합니다.", exception);
        }
    }

    /**
     * JWT 설정값을 검증한다.
     */
    private void validateConfiguration(String secret, long accessTokenExpiration, long refreshTokenExpiration) {
        if (!StringUtils.hasText(secret)) { throw new IllegalArgumentException("JWT Secret이 설정되지 않았습니다."); }
        if (accessTokenExpiration <= 0) { throw new IllegalArgumentException("Access Token 만료 시간은 0보다 커야 합니다."); }
        if (refreshTokenExpiration <= 0) { throw new IllegalArgumentException("Refresh Token 만료 시간은 0보다 커야 합니다."); }
    }

    private void validateUserId(UUID userId) {
        if (userId == null) { throw new IllegalArgumentException("회원 ID는 null일 수 없습니다."); }
    }

    private void validateUserStatus(UserStatus status) {
        if (status == null) { throw new IllegalArgumentException("회원 상태는 null일 수 없습니다."); }
    }

    private void validateClaims(Claims claims) {
        if (claims == null) { throw new IllegalArgumentException("JWT Claims는 null일 수 없습니다."); }
    }

}