package com.gm.api.security.oauth;

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.core.domain.auth.exception.AuthErrorCode;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        AuthErrorCode errorCode = AuthErrorCode.OAUTH_AUTHENTICATION_FAILED;

        log.warn(
                "[{}] 네이버 OAuth2 로그인에 실패했습니다. method={}, path={}, cause={}",
                errorCode.getCode(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );

        response.setStatus(errorCode.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // OAuth 로그인 실패 응답이 캐시되지 않도록 설정
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        ResponseEnvelope<Void> responseBody =
                ResponseEnvelope.fail(errorCode);

        objectMapper.writeValue(
                response.getWriter(),
                responseBody
        );
    }
}