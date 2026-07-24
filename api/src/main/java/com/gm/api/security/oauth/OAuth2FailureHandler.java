package com.gm.api.security.oauth;

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.core.domain.auth.exception.AuthErrorCode;
import com.gm.core.exception.CommonErrorCode;
import com.gm.core.exception.ErrorCode;

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

        ErrorCode errorCode = resolveErrorCode(exception);

        logFailure(request, exception, errorCode);

        response.setStatus(errorCode.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // OAuth 로그인 실패 응답이 캐시되지 않도록 설정
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        ResponseEnvelope<Void> responseBody = ResponseEnvelope.fail(errorCode);

        objectMapper.writeValue(response.getWriter(), responseBody);
    }

    /**
     * 인증 실패 원인에 따라 응답 오류 코드를 결정한다.
     * <p>회원 조회 또는 생성 중 발생한 서버 내부 오류는 {@link AuthenticationServiceException}으로 전달되므로 500으로 처리한다.</p>
     * <p>그 외 OAuth2 인증 실패는 401로 처리한다.</p>
     *
     * @param exception 인증 처리 중 발생한 예외
     * @return 예외에 대응하는 오류 코드
     */
    private ErrorCode resolveErrorCode(AuthenticationException exception) {
        if (exception instanceof AuthenticationServiceException) { return CommonErrorCode.INTERNAL_ERROR; }

        return AuthErrorCode.OAUTH_AUTHENTICATION_FAILED;
    }

    /**
     * 인증 실패 원인에 따라 로그 레벨과 내용을 구분한다.
     * <p>서버 내부 오류는 원인 추적을 위해 스택 트레이스와 함께 error 레벨로 기록한다.</p>
     * <p>일반 OAuth2 인증 실패는 내부 예외 정보를 노출하지 않고 warn 레벨로 기록한다.</p>
     *
     * @param request 실패한 HTTP 요청
     * @param exception 인증 처리 중 발생한 예외
     * @param errorCode 외부 응답에 사용할 오류 코드
     */
    private void logFailure(
            HttpServletRequest request,
            AuthenticationException exception,
            ErrorCode errorCode
    ) {
        if (exception instanceof AuthenticationServiceException) {
            log.error(
                    "[{}] OAuth2 로그인 처리 중 서버 오류가 발생했습니다. method={}, path={}",
                    errorCode.getCode(),
                    request.getMethod(),
                    request.getRequestURI(),
                    exception
            );
            return;
        }

        log.warn(
                "[{}] 네이버 OAuth2 로그인에 실패했습니다. method={}, path={}, cause={}",
                errorCode.getCode(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );
    }
}