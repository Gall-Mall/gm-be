package com.gm.api.security.oauth;

import java.io.IOException;

import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.gm.api.auth.dto.TokenResponse;
import com.gm.api.auth.service.AuthTokenService;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;

@Slf4j
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final String successRedirectUri;

    public OAuth2SuccessHandler(@Value("${app.oauth2.success-redirect-uri}") String successRedirectUri) {
        this.successRedirectUri = successRedirectUri;
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

        /*
         * PR3에서는 네이버 OAuth2 인증과 기존 회원 조회 또는 신규 회원 생성까지만 처리한다.
         * Access Token/Refresh Token 발급, Refresh Token 쿠키 설정, Redis 저장은 PR4에서 추가한다.
         */
        response.sendRedirect(successRedirectUri);
    }
}