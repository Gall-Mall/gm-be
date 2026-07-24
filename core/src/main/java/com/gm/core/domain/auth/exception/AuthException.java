package com.gm.core.domain.auth.exception;

import com.gm.core.exception.BusinessException;

/**
 * 인증 및 토큰 처리 과정에서 발생한 비즈니스 예외를 나타낸다.
 */
public class AuthException extends BusinessException {

    /**
     * 지정한 인증 오류 코드로 예외를 생성한다.
     *
     * @param authErrorCode 응답에 사용할 HTTP 상태, 오류 코드, 메시지
     */
    public AuthException(AuthErrorCode authErrorCode) { super(authErrorCode); }
}
