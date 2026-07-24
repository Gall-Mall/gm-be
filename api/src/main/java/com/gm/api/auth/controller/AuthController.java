package com.gm.api.auth.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import com.gm.api.auth.dto.AccessTokenResponse;
import com.gm.api.auth.dto.LoginExchangeRequest;
import com.gm.api.auth.service.AuthTokenService;
import com.gm.api.auth.service.AuthTokenService.IssuedAccessToken;
import com.gm.api.auth.service.LoginExchangeService;
import com.gm.api.auth.service.LogoutService;
import com.gm.api.auth.service.RefreshTokenCookieManager;
import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.auth.exception.AuthErrorCode;
import com.gm.core.domain.auth.exception.AuthException;
import com.gm.core.domain.auth.model.LoginExchange;

/**
 * OAuth 로그인 후 Access Token 발급, 재발급 및 로그아웃 API를 제공한다.
 * <p>Refresh Token은 응답 본문이나 요청 본문으로 주고받지 않고 HTTP-only 쿠키를 통해 전달한다.</p>
 * <p>Access Token은 로그인 교환 코드 검증 또는 유효한 Refresh Token 검증이 완료된 경우에만 응답 본문으로 반환한다.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";
    /*
     * 현재는 로그인 교환 코드와 Refresh Token의 userId 및 jti를 비교하기 위해 JwtProvider가 필요하다.
     * 향후 로그인 교환 전용 서비스로 해당 검증을 이동하면 컨트롤러에서 제거할 수 있다.
     */
    private final JwtProvider jwtProvider;
    private final AuthTokenService authTokenService;
    private final LoginExchangeService loginExchangeService;
    private final LogoutService logoutService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    /**
     * OAuth 로그인 교환 코드를 사용하여 Access Token을 발급한다.
     * <p>OAuth2SuccessHandler가 발급한 일회용 교환 코드와 HTTP-only 쿠키의 Refresh Token을 함께 검증한다.</p>
     * <p>교환 코드는 Redis에서 조회와 동시에 삭제되므로 한 번만 사용할 수 있다.</p>
     *
     * @param loginExchangeRequest 로그인 교환 코드 요청
     * @param request              현재 HTTP 요청
     * @return 발급된 Access Token
     */
    @PostMapping("/token")
    public ResponseEntity<ResponseEnvelope<AccessTokenResponse>> issueAccessToken(
            @Valid @RequestBody LoginExchangeRequest loginExchangeRequest, HttpServletRequest request
    ) {
        String refreshToken = resolveRequiredRefreshToken(request);

        /*
         * 교환 코드를 먼저 소비한다.
         * consume()은 Redis에서 조회와 삭제를 함께 수행하므로, 이후 검증에 실패하더라도 동일한 교환 코드는 다시 사용할 수 없다.
         */
        LoginExchange loginExchange = loginExchangeService.consume(loginExchangeRequest.code());
        // Refresh Token 검증과 교환 정보 일치 검증을 별도 메서드로 묶는다.
        RefreshTokenContext refreshTokenContext = validateLoginExchange(loginExchange, refreshToken);
        // AuthTokenService에서 Refresh Token의 Redis 저장 정보까지 검증하고, 현재 DB의 회원 상태를 반영한 Access Token을 발급한다.
        IssuedAccessToken issuedAccessToken = authTokenService.reissueAccessToken(refreshToken);

        // AccessTokenResponse 생성과 공통 응답 생성을 별도 메서드로 위임한다.
        ResponseEntity<ResponseEnvelope<AccessTokenResponse>> response = createAccessTokenResponse(issuedAccessToken);

        log.debug(
                "OAuth 로그인 Access Token 발급 완료: userId={}, refreshTokenId={}",
                refreshTokenContext.userId(), refreshTokenContext.refreshTokenId()
        );

        return response;
    }

    /**
     * Refresh Token을 사용하여 Access Token을 재발급한다.
     * <p>AUTH-003에서는 Refresh Token을 회전하지 않는다. 기존 Refresh Token 세션을 검증하고 새로운 Access Token만 발급한다.</p>
     *
     * @param request 현재 HTTP 요청
     * @return 재발급된 Access Token
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<ResponseEnvelope<AccessTokenResponse>> reissueAccessToken(HttpServletRequest request) {
        String refreshToken = resolveRequiredRefreshToken(request);
        IssuedAccessToken issuedAccessToken = authTokenService.reissueAccessToken(refreshToken);

        // /token과 동일한 응답 변환 로직을 재사용한다.
        return createAccessTokenResponse(issuedAccessToken);
    }

    /**
     * 현재 기기의 인증 세션을 로그아웃한다.
     * <p>요청의 Access Token은 남은 유효시간 동안 Redis 블랙리스트에 등록하고, 현재 Refresh Token의 Redis 데이터만 삭제한다.</p>
     * <p>로그아웃 완료 후 브라우저의 Refresh Token 쿠키도 삭제한다.</p>
     *
     * @param authorizationHeader Authorization 요청 헤더
     * @param request             현재 HTTP 요청
     * @param response            현재 HTTP 응답
     * @return 데이터가 없는 성공 응답
     */
    @PostMapping("/logout")
    public ResponseEntity<ResponseEnvelope<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorizationHeader,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String accessToken = resolveRequiredAccessToken(authorizationHeader);
        String refreshToken = resolveRequiredRefreshToken(request);

        logoutService.logout(accessToken, refreshToken);
        // 서버 측 로그아웃 처리가 정상적으로 완료된 후에만클라이언트의 Refresh Token 쿠키를 삭제한다.
        refreshTokenCookieManager.deleteRefreshTokenCookie(response);

        return ResponseEntity.ok(ResponseEnvelope.success(null));
    }

    /**
     * 로그인 교환 정보와 쿠키의 Refresh Token이 같은 로그인 세션에 속하는지 검증한다.
     * <p>다음 항목을 확인한다.</p>
     * <p>1. Refresh Token JWT 유효성</p>
     * <p>2. 교환 정보의 refreshTokenId와 JWT jti 일치 여부</p>
     * <p>3. 교환 정보의 userId와 JWT subject 일치 여부</p>
     *
     * @param loginExchange 소비한 로그인 교환 정보
     * @param refreshToken  쿠키에서 조회한 Refresh Token
     * @return 검증된 회원 UUID와 Refresh Token ID
     */
    private RefreshTokenContext validateLoginExchange(LoginExchange loginExchange, String refreshToken) {
        Claims refreshTokenClaims = validateRefreshToken(refreshToken);
        UUID refreshTokenUserId = jwtProvider.getUserId(refreshTokenClaims);
        String refreshTokenId = jwtProvider.getJti(refreshTokenClaims);

        loginExchangeService.validateRefreshTokenSession(loginExchange, refreshTokenId);

        validateLoginExchangeUser(loginExchange, refreshTokenUserId);

        return new RefreshTokenContext(refreshTokenUserId, refreshTokenId);
    }

    /**
     * 발급된 서비스 계층 Access Token 정보를 API 응답 DTO로 변환한다.
     *
     * @param issuedAccessToken 서비스 계층의 Access Token 발급 결과
     * @return 공통 응답으로 감싼 Access Token 응답
     */
    private ResponseEntity<ResponseEnvelope<AccessTokenResponse>>
    createAccessTokenResponse(IssuedAccessToken issuedAccessToken) {

        if (issuedAccessToken == null) { throw new IllegalStateException("발급된 Access Token 정보는 null일 수 없습니다."); }

        AccessTokenResponse accessTokenResponse =
                AccessTokenResponse.of(issuedAccessToken.token(), issuedAccessToken.expiresIn());

        return ResponseEntity.ok(ResponseEnvelope.success(accessTokenResponse));
    }


    /**
     * HTTP-only 쿠키에서 Refresh Token을 조회한다.
     *
     * @param request 현재 HTTP 요청
     * @return 쿠키에 저장된 Refresh Token
     */
    private String resolveRequiredRefreshToken(HttpServletRequest request) {
        return refreshTokenCookieManager
                .resolveRefreshToken(request)
                .orElseThrow(() ->
                        new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }

    /**
     * Authorization 헤더에서 Bearer Access Token을 추출한다.
     *
     * @param authorizationHeader Authorization 요청 헤더
     * @return Bearer 접두사를 제거한 Access Token
     */
    private String resolveRequiredAccessToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) { throw new AuthException(AuthErrorCode.ACCESS_TOKEN_NOT_FOUND); }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) { throw new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN); }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());

        if (!StringUtils.hasText(accessToken)) { throw new AuthException(AuthErrorCode.ACCESS_TOKEN_NOT_FOUND); }

        return accessToken;
    }

    /**
     * Refresh Token의 서명, 만료 시간 및 토큰 유형을 검증한다.
     *
     * @param refreshToken 검증할 Refresh Token
     * @return 검증된 JWT Claims
     */
    private Claims validateRefreshToken(
            String refreshToken
    ) {
        try {
            return jwtProvider.validateRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("로그인 교환 Refresh Token 검증에 실패했습니다. cause={}", exception.getClass().getSimpleName());

            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * 로그인 교환 코드의 회원과 Refresh Token의 회원이 일치하는지 확인한다.
     *
     * @param loginExchange      소비한 로그인 교환 정보
     * @param refreshTokenUserId Refresh Token의 회원 UUID
     */
    private void validateLoginExchangeUser(LoginExchange loginExchange, UUID refreshTokenUserId) {
        if (loginExchange == null) { throw new AuthException(AuthErrorCode.INVALID_LOGIN_EXCHANGE_CODE); }

        if (refreshTokenUserId == null || !loginExchange.userId().equals(refreshTokenUserId)) {

            log.warn(
                    "로그인 교환 코드와 Refresh Token의 회원이 일치하지 않습니다. " + "exchangeUserId={}, refreshTokenUserId={}",
                    loginExchange.userId(), refreshTokenUserId
            );

            throw new AuthException(AuthErrorCode.TOKEN_USER_MISMATCH);
        }
    }

    /**
     * 검증된 Refresh Token의 식별 정보를 나타낸다.
     *
     * @param userId         Refresh Token에 포함된 회원 UUID
     * @param refreshTokenId Refresh Token의 JWT ID
     */
    private record RefreshTokenContext(UUID userId, String refreshTokenId) {
        private RefreshTokenContext {
            if (userId == null) { throw new IllegalArgumentException("Refresh Token 회원 ID는 null일 수 없습니다."); }
            if (!StringUtils.hasText(refreshTokenId)) {
                throw new IllegalArgumentException("Refresh Token ID는 null이거나 공백일 수 없습니다.");
            }
        }
    }
}