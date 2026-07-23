package com.gm.api.security.jwt;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.gm.core.domain.user.model.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String STATUS_CLAIM = "status";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";

    private final SecretKey key;
    private final long accessTokenExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration
    ) {
        if (!StringUtils.hasText(secret)) { throw new IllegalArgumentException("JWT Secret이 설정되지 않았습니다."); }
        if (accessTokenExpiration <= 0) { throw new IllegalArgumentException("Access Token 만료 시간은 0보다 커야 합니다."); }

        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.accessTokenExpiration = accessTokenExpiration;
    }

    /**
     * 서비스 Access Token을 생성한다.
     *
     * @param userId 서비스 회원 UUID
     * @param status 사용자 상태
     * @return 생성된 Access Token
     */
    public String createAccessToken(UUID userId, UserStatus status) {

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
     * Access Token을 검증하고 Claims를 반환한다.
     *
     * 검증 항목
     * - 토큰 존재 여부
     * - JWT 형식
     * - 서명
     * - 만료 시간
     * - Access Token 여부
     *
     * @param token Access Token
     * @return JWT Claims
     */
    public Claims validate(String token) {

        if (!StringUtils.hasText(token)) { throw new IllegalArgumentException("Access Token이 존재하지 않습니다."); }

        Claims claims = parseClaims(token);
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        if (!ACCESS_TOKEN_TYPE.equals(tokenType)) { throw new JwtException("Access Token 타입이 올바르지 않습니다."); }

        return claims;
    }

    /**
     * Claims에서 회원 UUID를 반환한다.
     */
    public UUID getUserId(Claims claims) {

        String subject = claims.getSubject();

        if (!StringUtils.hasText(subject)) { throw new JwtException("회원 식별자가 존재하지 않습니다."); }

        return UUID.fromString(subject);
    }

    /**
     * Claims에서 회원 상태를 반환한다.
     */
    public UserStatus getUserStatus(Claims claims) {

        String status = claims.get(STATUS_CLAIM, String.class);

        if (!StringUtils.hasText(status)) { throw new JwtException("회원 상태가 존재하지 않습니다."); }

        return UserStatus.valueOf(status);
    }

    /**
     * JWT ID(jti)를 반환한다.
     */
    public String getJti(Claims claims) {

        String jti = claims.getId();

        if (!StringUtils.hasText(jti)) { throw new JwtException("JWT ID가 존재하지 않습니다."); }

        return jti;
    }

    /**
     * Access Token 만료 시간을 초 단위로 반환한다.
     */
    public long getAccessTokenExpirationSeconds() { return accessTokenExpiration; }

    /**
     * JWT 서명 및 만료 시간을 검증한 뒤 Claims를 반환한다.
     */
    private Claims parseClaims(String token) {

        return Jwts.parser()
                .verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}