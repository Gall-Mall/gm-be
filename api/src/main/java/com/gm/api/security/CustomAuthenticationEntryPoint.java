package com.gm.api.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import tools.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.core.exception.CommonErrorCode;

/**
 * 인증되지 않은 사용자의 요청을 공통 실패 응답으로 변환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * 인증 예외를 공통 실패 응답으로 변환한다.
     *
     * @param request 현재 HTTP 요청
     * @param response 반환할 HTTP 응답
     * @param authException 발생한 인증 예외
     * @throws IOException 응답 작성에 실패한 경우
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        log.warn(
                "[{}] 인증되지 않은 요청입니다. method={}, path={}",
                CommonErrorCode.UNAUTHORIZED.getCode(),
                request.getMethod(),
                request.getRequestURI()
        );

        response.setStatus(CommonErrorCode.UNAUTHORIZED.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ResponseEnvelope<Void> responseBody =
                ResponseEnvelope.fail(CommonErrorCode.UNAUTHORIZED);

        objectMapper.writeValue(
                response.getWriter(),
                responseBody
        );
    }
}