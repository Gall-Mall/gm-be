package com.gm.api.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import tools.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.core.exception.CommonErrorCode;
import com.gm.core.exception.ErrorCode;

/**
 * 인증된 사용자가 접근 권한이 없는 자원에 요청한 경우 공통 403 실패 응답을 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * 접근 거부 예외를 공통 실패 응답으로 변환한다.
     *
     * @param request 현재 HTTP 요청
     * @param response 반환할 HTTP 응답
     * @param accessDeniedException 발생한 접근 거부 예외
     * @throws IOException 응답 본문 작성에 실패한 경우
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        ErrorCode errorCode = CommonErrorCode.ACCESS_DENIED;

        log.warn(
                "[{}] 접근 권한이 없는 요청입니다. method={}, path={}",
                errorCode.getCode(),
                request.getMethod(),
                request.getRequestURI()
        );

        response.setStatus(errorCode.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ResponseEnvelope<Void> responseBody =
                ResponseEnvelope.fail(errorCode);

        objectMapper.writeValue(
                response.getWriter(),
                responseBody
        );
    }
}