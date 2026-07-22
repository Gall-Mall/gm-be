package com.gm.core.domain.user.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.gm.core.exception.ErrorCode;

/**
 * 사용자 도메인에서 사용하는 오류 코드다.
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(404,"USER-001","사용자를 찾을 수 없습니다."),
    INVALID_ONBOARDING_STATUS(409,"USER-002","현재 온보딩 상태에서는 요청을 처리할 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}