package com.gm.core.domain.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.gm.core.exception.ErrorCode;

/**
 * 인증 도메인의 오류 코드다.
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    /**
     * OAuth 제공자의 인증 또는 사용자 정보 조회가 실패한 경우.
     */
    OAUTH_AUTHENTICATION_FAILED(401, "AUTH-004", "소셜 로그인에 실패했습니다.");

    private final int status;
    private final String code;
    private final String message;
}