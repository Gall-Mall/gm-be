package com.gm.client.kakao.exception;

import com.gm.core.exception.ErrorCode;

public enum KakaoErrorCode implements ErrorCode {
    KAKAO_API_ERROR(502, "KAKAOMAP-001", "카카오API 호출 에러가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;

    KakaoErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
