package com.gm.api.security.oauth;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;


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

        // 실패 응답이 캐시되지 않도록 설정
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> responseBody = new LinkedHashMap<>();

        responseBody.put("status", HttpStatus.UNAUTHORIZED.value());
        responseBody.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        responseBody.put("code", resolveErrorCode(exception));
        responseBody.put("message", "네이버 로그인에 실패했습니다.");
        responseBody.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), responseBody);
    }

    /**
     * OAuth2 예외인 경우 OAuth2 오류 코드를,
     * 그 외에는 공통 오류 코드를 반환한다.
     */
    private String resolveErrorCode(AuthenticationException exception) {

        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            return oauth2Exception.getError().getErrorCode();
        }

        return "OAUTH2_AUTHENTICATION_FAILED";
    }
}