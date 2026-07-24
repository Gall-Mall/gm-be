package com.gm.api.auth.service;

import java.time.Duration;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.auth.exception.AuthErrorCode;
import com.gm.core.domain.auth.exception.AuthException;
import com.gm.core.domain.auth.repository.AccessTokenBlacklistRepository;
import com.gm.core.domain.auth.repository.RefreshTokenRepository;

/**
 * 현재 기기의 인증 세션 로그아웃을 처리한다.
 * <p>Access Token은 남은 유효시간 동안 Redis 블랙리스트에 등록한다.</p>
 * <p>Refresh Token은 현재 쿠키에 포함된 토큰의 jti에 해당하는 Redis 데이터만 삭제한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    /**
     * 현재 기기에서 로그아웃한다.
     *
     * @param accessToken  Authorization 헤더의 Access Token
     * @param refreshToken HTTP-only 쿠키의 Refresh Token
     */
    public void logout(String accessToken, String refreshToken) {
        if (!StringUtils.hasText(accessToken)) { throw new AuthException(AuthErrorCode.ACCESS_TOKEN_NOT_FOUND); }
        if (!StringUtils.hasText(refreshToken)) { throw new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND); }

        Claims accessClaims = validateAccessToken(accessToken);
        Claims refreshClaims = validateRefreshToken(refreshToken);
        UUID accessTokenUserId = jwtProvider.getUserId(accessClaims);
        UUID refreshTokenUserId = jwtProvider.getUserId(refreshClaims);

        if (!accessTokenUserId.equals(refreshTokenUserId)) {
            log.warn(
                    "Access Token과 Refresh Token의 회원이 일치하지 않습니다. " + "accessUserId={}, refreshUserId={}",
                    accessTokenUserId, refreshTokenUserId
            );

            throw new AuthException(AuthErrorCode.TOKEN_USER_MISMATCH);
        }

        String accessTokenId = jwtProvider.getJti(accessClaims);
        String refreshTokenId = jwtProvider.getJti(refreshClaims);
        boolean refreshTokenMatched = refreshTokenRepository.matches(refreshTokenId, refreshTokenUserId, refreshToken);

        if (!refreshTokenMatched) {
            log.debug(
                    "로그아웃 요청의 Refresh Token 세션이 유효하지 않습니다. " + "userId={}, refreshTokenId={}",
                    refreshTokenUserId, refreshTokenId
            );

            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        blacklistAccessToken(accessTokenId, accessClaims);

        refreshTokenRepository.delete(refreshTokenId);

        log.debug(
                "현재 기기 로그아웃을 완료했습니다. userId={}, " + "accessTokenId={}, refreshTokenId={}",
                accessTokenUserId, accessTokenId, refreshTokenId
        );
    }

    /**
     * Access Token의 남은 유효시간만큼 블랙리스트에 저장한다.
     */
    private void blacklistAccessToken(String accessTokenId, Claims accessClaims) {
        // 동일 Access Token으로 로그아웃 API가 중복 호출되더라도 이미 블랙리스트에 존재하면 다시 저장하지 않는다.
        if (accessTokenBlacklistRepository.exists(accessTokenId)) { return; }

        Duration remainingExpiration = jwtProvider.getRemainingExpiration(accessClaims);

        accessTokenBlacklistRepository.save(accessTokenId, remainingExpiration);
    }

    private Claims validateAccessToken(String accessToken) {
        try {
            return jwtProvider.validateAccessToken(accessToken);
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("로그아웃 Access Token 검증에 실패했습니다. cause={}", exception.getClass().getSimpleName());

            throw new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }
    }

    private Claims validateRefreshToken(String refreshToken) {
        try {
            return jwtProvider.validateRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("로그아웃 Refresh Token 검증에 실패했습니다. cause={}", exception.getClass().getSimpleName());

            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}