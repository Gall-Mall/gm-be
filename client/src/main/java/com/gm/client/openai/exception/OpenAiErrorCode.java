package com.gm.client.openai.exception;

import com.gm.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OpenAiErrorCode implements ErrorCode {
    OPENAI_API_ERROR(502, "OPENAI-001", "OpenAI API 호출 에러가 발생했습니다."),
    OPENAI_RESPONSE_ERROR(502, "OPENAI-002", "OpenAI API 응답 데이터가 올바르지 않습니다.");

    private final int status;
    private final String code;
    private final String message;
}
