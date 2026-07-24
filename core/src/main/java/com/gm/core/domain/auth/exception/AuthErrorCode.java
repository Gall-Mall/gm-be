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

    OAUTH_AUTHENTICATION_FAILED(401, "AUTH-004", "소셜 로그인에 실패했습니다."),
    ACCESS_TOKEN_NOT_FOUND(401, "AUTH-005", "Access Token이 존재하지 않습니다."),
    REFRESH_TOKEN_NOT_FOUND(401, "AUTH-006", "Refresh Token이 존재하지 않습니다."),
    INVALID_ACCESS_TOKEN(401, "AUTH-007", "유효하지 않은 Access Token입니다."),
    INVALID_REFRESH_TOKEN(401, "AUTH-008", "유효하지 않은 Refresh Token입니다."),
    AUTHENTICATED_USER_NOT_FOUND(401, "AUTH-009", "인증된 회원 정보를 찾을 수 없습니다."),
    WITHDRAWN_USER(403, "AUTH-010", "탈퇴한 회원은 서비스를 이용할 수 없습니다."),
    LOGIN_EXCHANGE_CODE_NOT_FOUND(400, "AUTH-011", "로그인 교환 코드가 존재하지 않습니다."),
    INVALID_LOGIN_EXCHANGE_CODE(401, "AUTH-012", "유효하지 않은 로그인 교환 코드입니다."),
    LOGIN_EXCHANGE_SESSION_MISMATCH(401, "AUTH-013", "로그인 세션이 일치하지 않습니다."),
    TOKEN_USER_MISMATCH(401, "AUTH-014", "인증 정보의 회원이 일치하지 않습니다.");

    private final int status;
    private final String code;
    private final String message;
}