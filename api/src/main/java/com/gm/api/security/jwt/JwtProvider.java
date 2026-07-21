package com.gm.api.security.jwt;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";

    private final SecretKey key;
    private final long accessTokenExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration
    ) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.accessTokenExpiration = accessTokenExpiration;
    }

    /**
     * 서비스 Access Token을 생성한다.
     *
     * @param userId 서비스 회원 UUID
     * @return 생성된 Access Token
     */
    public String createAccessToken(UUID userId) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plusSeconds(accessTokenExpiration);

        return Jwts.builder()
                // JWT 고유 식별자
                .id(UUID.randomUUID().toString())
                // 서비스 회원 UUID
                .subject(userId.toString())
                // Access Token임을 나타내는 Claim
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                // 토큰 발급 시각
                .issuedAt(Date.from(issuedAt))
                // 토큰 만료 시각
                .expiration(Date.from(expiration))
                // JWT 서명
                .signWith(key)
                .compact();
    }

    /**
     * Access Token의 유효성을 검증한다.
     *
     * 검증 항목:
     * - 토큰 존재 여부
     * - JWT 형식
     * - JWT 서명
     * - JWT 만료 시간
     * - JWT 변조 여부
     * - Access Token 타입 여부
     */
    public boolean validateAccessToken(String token) {
        if (token == null || token.isBlank()) { return false; }

        try {
            Claims claims = parseClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

            return ACCESS_TOKEN_TYPE.equals(tokenType);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Access Token의 subject에서 서비스 회원 UUID를 조회한다.
     *
     * @param token Access Token
     * @return 서비스 회원 UUID
     */
    public UUID getUserId(String token) {
        String subject = parseClaims(token).getSubject();

        return UUID.fromString(subject);
    }

    /** Access Token 만료 시간을 초 단위로 반환한다. */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration;
    }

    /**
     * JWT 서명과 만료 시간을 검증한 뒤 Claims를 반환한다.
     * 잘못된 서명, 만료된 토큰, 변조된 토큰이면 JwtException 계열 예외가 발생한다.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}