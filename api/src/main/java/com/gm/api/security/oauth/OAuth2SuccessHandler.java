package com.gm.api.security.oauth;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.model.UserStatus;

@Slf4j
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final String onboardingRedirectUri;
    private final String homeRedirectUri;

    public OAuth2SuccessHandler(
            @Value("${app.oauth2.onboarding-redirect-uri}") String onboardingRedirectUri,
            @Value("${app.oauth2.home-redirect-uri}") String homeRedirectUri
    ) {
        this.onboardingRedirectUri = onboardingRedirectUri;
        this.homeRedirectUri = homeRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        UserStatus status = principal.getUser().status();

        log.info("네이버 OAuth2 인증 완료: userId={}, status={}", principal.getUserId(), status);

        // OAuth 로그인 성공 응답이 캐시되지 않도록 설정
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        // 온보딩 미완료(ONBOARDING)면 온보딩 화면, 그 외(ACTIVE 등)는 메인 화면으로 보낸다.
        String redirectUri = status == UserStatus.ONBOARDING ? onboardingRedirectUri : homeRedirectUri;

        /*
         * PR3에서는 네이버 OAuth2 인증과 기존 회원 조회 또는 신규 회원 생성까지만 처리한다.
         * Access Token/Refresh Token 발급, Refresh Token 쿠키 설정, Redis 저장은 PR4에서 추가한다.
         */
        response.sendRedirect(redirectUri);
    }
}