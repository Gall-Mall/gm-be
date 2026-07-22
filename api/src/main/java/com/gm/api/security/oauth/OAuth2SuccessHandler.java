package com.gm.api.security.oauth;

import java.io.IOException;

import com.gm.api.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.gm.api.auth.dto.TokenResponse;
import com.gm.api.auth.service.AuthTokenService;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthTokenService authTokenService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        TokenResponse tokenResponse = authTokenService.issue(principal.getUserId());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // 로그인 응답은 캐시하지 않는다.
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        objectMapper.writeValue(response.getWriter(), tokenResponse);
    }
}