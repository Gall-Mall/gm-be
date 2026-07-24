package com.gm.api.auth.service;

import java.time.Duration;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.auth.exception.AuthErrorCode;
import com.gm.core.domain.auth.exception.AuthException;
import com.gm.core.domain.auth.repository.RefreshTokenRepository;
import com.gm.core.domain.user.exception.UserException;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.service.UserService;

/**
 * Access Token과 Refresh Token의 발급 및 재발급을 담당한다.
 * <p>Refresh Token은 Redis에 저장되며, 원문 대신 해시값이 저장된다.</p>
 * <p>Access Token 재발급 시 기존 Refresh Token을 그대로 사용한다. 즉, Refresh Token Rotation은 적용하지 않는다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtProvider jwtProvider;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * OAuth 로그인 성공 후 사용할 Refresh Token을 신규 발급한다.
     * <p>발급한 Refresh Token은 Redis에 저장되며, Refresh Token의 jti가 현재 기기 로그인 세션 식별자로 사용된다.</p>
     *
     * @param userId 서비스 회원 UUID
     * @return 발급된 Refresh Token 정보
     */
    public IssuedRefreshToken issueRefreshToken(UUID userId) {

        validateUserId(userId);
        User user = findAvailableUser(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);
        Claims claims = jwtProvider.validateRefreshToken(refreshToken);
        String refreshTokenId = jwtProvider.getJti(claims);
        Duration expiration = jwtProvider.getRefreshTokenExpiration();

        refreshTokenRepository.save(refreshTokenId, userId, refreshToken, expiration);

        log.debug(
                "Refresh Token을 발급했습니다. userId={}, refreshTokenId={}, status={}",
                userId, refreshTokenId, user.status()
        );

        return new IssuedRefreshToken(refreshToken, refreshTokenId, expiration);
    }

    /**
     * 회원 UUID를 기준으로 Access Token을 발급한다.
     * <p>OAuth 로그인 교환 코드가 정상적으로 소비된 후 호출한다.</p>
     * <p>Access Token에 포함되는 회원 상태는 현재 DB의 상태를 사용한다.</p>
     *
     * @param userId 서비스 회원 UUID
     * @return 발급된 Access Token 정보
     */
    @Transactional(readOnly = true)
    public IssuedAccessToken issueAccessToken(UUID userId) {

        validateUserId(userId);
        User user = findAvailableUser(userId);
        // 토큰 생성과 응답용 만료 시간 계산을 공통 메서드로 위임한다.
        IssuedAccessToken issuedAccessToken = createIssuedAccessToken(userId, user.status());

        log.debug("Access Token을 발급했습니다. userId={}, status={}", userId, user.status());

        return issuedAccessToken;
    }

    /**
     * Refresh Token을 검증하고 Access Token을 재발급한다.
     * <p>Refresh Token 자체의 JWT 검증뿐 아니라 Redis에 저장된 현재 로그인 세션과 일치하는지도 확인한다.</p>
     * <p>재발급 과정에서 새로운 Refresh Token을 발급하지 않는다.</p>
     *
     * @param refreshToken 쿠키에서 전달받은 Refresh Token
     * @return 재발급된 Access Token 정보
     */
    @Transactional(readOnly = true)
    public IssuedAccessToken reissueAccessToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) { throw new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND); }

        Claims claims = validateRefreshToken(refreshToken);
        UUID userId = jwtProvider.getUserId(claims);
        String refreshTokenId = jwtProvider.getJti(claims);
        boolean matched = refreshTokenRepository.matches(refreshTokenId, userId, refreshToken);

        if (!matched) {
            log.debug("Redis Refresh Token 검증에 실패했습니다. userId={}, refreshTokenId={}", userId, refreshTokenId);

            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = findAvailableUser(userId);
        // Access Token과 expiresIn을 함께 생성한다.
        IssuedAccessToken issuedAccessToken = createIssuedAccessToken(userId, user.status());

        log.debug(
                "Access Token을 재발급했습니다. userId={}, refreshTokenId={}, status={}",
                userId, refreshTokenId, user.status()
        );

        return issuedAccessToken;
    }

    /**
     * Access Token을 생성하고 클라이언트 응답에 사용할 만료 시간을 함께 반환한다.
     * <p>Access Token 만료 시간은 JwtProvider에 설정된 Access Token 만료 시간 값을 사용한다.</p>
     *
     * @param userId 회원 UUID
     * @param status 현재 회원 상태
     * @return Access Token 원문과 만료 시간
     */
    private IssuedAccessToken createIssuedAccessToken(UUID userId, UserStatus status) {
        String accessToken = jwtProvider.createAccessToken(userId, status);
        // 현재 JwtProvider가 Access Token 만료 설정값을 초 단위로 제공하므로 해당 값을 직접 사용한다.
        long expiresIn = jwtProvider.getAccessTokenExpirationSeconds();

        return new IssuedAccessToken(accessToken, expiresIn);
    }

    /**
     * Refresh Token을 검증한다.
     * <p>JWT 라이브러리 예외가 API 계층 밖으로 직접 노출되지 않도록 AuthException으로 변환한다.</p>
     *
     * @param refreshToken 검증할 Refresh Token
     * @return 검증된 Refresh Token Claims
     */
    private Claims validateRefreshToken(String refreshToken) {
        try {
            return jwtProvider.validateRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("Refresh Token JWT 검증에 실패했습니다. cause={}", exception.getClass().getSimpleName());

            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * Access Token을 발급할 수 있는 회원인지 확인한다.
     * <p>ONBOARDING 회원과 ACTIVE 회원은 허용하고, WITHDRAWN 회원은 거부한다.</p>
     *
     * @param userId 회원 UUID
     * @return 조회된 사용 가능한 회원
     */
    private User findAvailableUser(UUID userId) {
        User user;

        try {
            user = userService.findById(userId);
        } catch (UserException exception) {
            throw new AuthException(AuthErrorCode.AUTHENTICATED_USER_NOT_FOUND);
        }

        if (user.status() == UserStatus.WITHDRAWN) { throw new AuthException(AuthErrorCode.WITHDRAWN_USER);}

        return user;
    }

    /**
     * 회원 UUID 필수값을 검증한다.
     *
     * @param userId 회원 UUID
     */
    private void validateUserId(UUID userId) {
        if (userId == null) { throw new IllegalArgumentException("회원 ID는 null일 수 없습니다."); }
    }

    /**
     * 신규 발급된 Access Token 정보를 나타낸다.
     * <p>이 값은 서비스 계층의 반환 모델이며, 컨트롤러에서 AccessTokenResponse로 변환한다.</p>
     *
     * @param token     Access Token 원문
     * @param expiresIn Access Token의 남은 유효 시간(초)
     */
    public record IssuedAccessToken(String token, long expiresIn) {

        public IssuedAccessToken {
            if (!StringUtils.hasText(token)) {
                throw new IllegalArgumentException("Access Token은 null이거나 공백일 수 없습니다.");
            }
            if (expiresIn <= 0) {
                throw new IllegalArgumentException("Access Token 만료 시간은 0보다 커야 합니다.");
            }
        }
    }

    /**
     * 신규 발급된 Refresh Token 정보를 나타낸다.
     * <p>이 값은 서비스 계층의 반환 모델이며, 컨트롤러에서 AccessTokenResponse로 변환한다.</p>
     *
     * @param token          Refresh Token 원문
     * @param refreshTokenId Refresh Token의 JWT ID
     * @param expiration     Refresh Token 유효기간
     */
    public record IssuedRefreshToken(String token, String refreshTokenId, Duration expiration) {
        public IssuedRefreshToken {
            if (!StringUtils.hasText(token)) {
                throw new IllegalArgumentException("Refresh Token은 null이거나 공백일 수 없습니다.");
            }
            if (!StringUtils.hasText(refreshTokenId)) {
                throw new IllegalArgumentException("Refresh Token ID는 null이거나 공백일 수 없습니다.");
            }
            if (expiration == null || expiration.isZero() || expiration.isNegative()) {
                throw new IllegalArgumentException("Refresh Token 만료 시간은 0보다 커야 합니다.");
            }
        }
    }
}