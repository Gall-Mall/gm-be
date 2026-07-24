package com.gm.api.security.oauth;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.gm.api.auth.service.AuthTokenService;
import com.gm.api.auth.service.AuthTokenService.IssuedRefreshToken;
import com.gm.api.auth.service.LoginExchangeService;
import com.gm.api.auth.service.RefreshTokenCookieManager;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.model.UserStatus;

/**
 * 네이버 OAuth2 인증 성공 후 후속 로그인 교환 절차를 처리한다.
 * <p>OAuth2 인증이 완료되면 Refresh Token을 발급하여 HTTP-only 쿠키에 저장한다.</p>
 * <p>이후 회원 UUID와 Refresh Token 세션 식별자를 Redis에 일회용 로그인 교환 정보로 저장하고, 프런트엔드에는 Access Token 대신 일회용 교환 코드만 전달한다.</p>
 */
@Slf4j
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final String EXCHANGE_CODE_QUERY_PARAM = "code";
    private final String onboardingRedirectUri;
    private final String homeRedirectUri;
    private final AuthTokenService authTokenService;
    private final LoginExchangeService loginExchangeService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    /**
     * OAuth2 인증 성공 처리기를 생성한다.
     *
     * @param onboardingRedirectUri     온보딩 미완료(ONBOARDING) 회원이 이동할 프런트엔드 URI
     * @param homeRedirectUri           온보딩 완료(ACTIVE 등) 회원이 이동할 프런트엔드 URI
     * @param authTokenService          토큰 발급 서비스
     * @param loginExchangeService      로그인 교환 코드 관리 서비스
     * @param refreshTokenCookieManager Refresh Token 쿠키 관리 컴포넌트
     */
    public OAuth2SuccessHandler(
            @Value("${app.oauth2.onboarding-redirect-uri}") String onboardingRedirectUri,
            @Value("${app.oauth2.home-redirect-uri}") String homeRedirectUri,
            AuthTokenService authTokenService,
            LoginExchangeService loginExchangeService,
            RefreshTokenCookieManager refreshTokenCookieManager) {
        this.onboardingRedirectUri = onboardingRedirectUri;
        this.homeRedirectUri = homeRedirectUri;
        this.authTokenService = authTokenService;
        this.loginExchangeService = loginExchangeService;
        this.refreshTokenCookieManager = refreshTokenCookieManager;
    }

    /**
     * 네이버 OAuth2 인증 성공 후 Refresh Token과 로그인 교환 코드를 발급한다.
     * <p>Access Token은 OAuth2 Redirect 응답에 포함하지 않는다.</p>
     * <p>프런트엔드는 Redirect URI에 포함된 일회용 교환 코드를 후속 인증 API에 전달하여 Access Token을 발급받는다.</p>
     *
     * @param request        현재 HTTP 요청
     * @param response       현재 HTTP 응답
     * @param authentication OAuth2 인증 결과
     * @throws IOException Redirect 처리에 실패한 경우
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication
    ) throws IOException {

        CustomUserPrincipal principal = resolvePrincipal(authentication);
        UUID userId = principal.getUserId();
        UserStatus status = principal.getUser().status();

        log.info("네이버 OAuth2 인증 완료: userId={}, status={}", principal.getUserId(), status);

        setNoCacheHeaders(response);
        // OAuth 로그인 성공 후 현재 기기 세션에 사용할 Refresh Token을 신규 발급하고 Redis에 저장한다.
        IssuedRefreshToken issuedRefreshToken = authTokenService.issueRefreshToken(userId);
        // Refresh Token 원문은 Redirect URI나 응답 본문에 포함하지 않고 HTTP-only 쿠키로만 전달한다.
        refreshTokenCookieManager.addRefreshTokenCookie(response, issuedRefreshToken.token());
        // Access Token 대신 사용할 일회용 로그인 교환 코드를 생성한다. Redis에는 userId와 Refresh Token의 jti만 저장된다.
        String exchangeCode = loginExchangeService.createExchangeCode(userId, issuedRefreshToken.refreshTokenId());

        // 회원 상태에 따라 온보딩/메인 화면으로 보내되, 일회용 로그인 교환 코드를 쿼리 파라미터로 전달한다.
        String redirectUri = createSuccessRedirectUri(exchangeCode, status);

        log.debug(
                "OAuth2 로그인 교환 코드 발급 완료: " + "userId={}, refreshTokenId={}",
                userId, issuedRefreshToken.refreshTokenId()
        );

        response.sendRedirect(redirectUri);
    }

    /**
     * 인증 결과에서 서비스 사용자 Principal을 조회한다.
     *
     * @param authentication OAuth2 인증 결과
     * @return 서비스 사용자 Principal
     */
    private CustomUserPrincipal resolvePrincipal(Authentication authentication) {
        if (authentication == null) { throw new IllegalArgumentException("Authentication은 null일 수 없습니다."); }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserPrincipal customUserPrincipal)) {
            throw new IllegalStateException("지원하지 않는 OAuth2 Principal 타입입니다.");
        }

        return customUserPrincipal;
    }

    /**
     * OAuth 로그인 성공 Redirect URI를 생성한다.
     * <p>회원 상태에 따라 온보딩/메인 화면 URI를 고르고, 기존 URI에 쿼리 파라미터가 있더라도
     * 안전하게 로그인 교환 코드를 추가한다.</p>
     *
     * @param exchangeCode 일회용 로그인 교환 코드
     * @param status       회원 상태 (ONBOARDING이면 온보딩, 그 외는 메인)
     * @return 교환 코드가 포함된 Redirect URI
     */
    private String createSuccessRedirectUri(String exchangeCode, UserStatus status) {
        String baseUri = status == UserStatus.ONBOARDING ? onboardingRedirectUri : homeRedirectUri;
        return UriComponentsBuilder
                .fromUriString(baseUri)
                .queryParam(EXCHANGE_CODE_QUERY_PARAM, exchangeCode)
                .build()
                .encode()
                .toUriString();
    }

    /**
     * OAuth 로그인 성공 응답이 브라우저 또는 프록시에 캐시되지 않도록 설정한다.
     *
     * @param response 현재 HTTP 응답
     */
    private void setNoCacheHeaders(HttpServletResponse response) {
        if (response == null) { throw new IllegalArgumentException("HTTP 응답은 null일 수 없습니다."); }

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
    }
}