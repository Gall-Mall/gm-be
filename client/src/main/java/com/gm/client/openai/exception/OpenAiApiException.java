package com.gm.client.openai.exception;

import com.gm.core.exception.BusinessException;

public class OpenAiApiException extends BusinessException {

    public OpenAiApiException(OpenAiErrorCode errorCode) {
        super(errorCode);
    }
}
