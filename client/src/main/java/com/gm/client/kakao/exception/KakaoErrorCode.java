package com.gm.client.kakao.exception;

import com.gm.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum KakaoErrorCode implements ErrorCode {
    KAKAO_API_ERROR(502, "KAKAOMAP-001", "카카오API 호출 에러가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
