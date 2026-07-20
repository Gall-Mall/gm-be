package com.gm.client.kakao.exception;

import com.gm.core.exception.BusinessException;

public class KakaoApiException extends BusinessException {

    public KakaoApiException(KakaoErrorCode errorCode) {
        super(errorCode);
    }
}
