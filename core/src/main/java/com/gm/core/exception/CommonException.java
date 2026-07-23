package com.gm.core.exception;

/**
 * 특정 도메인에 속하지 않는 공통 오류를 나타낸다.
 */
public class CommonException extends BusinessException {

    /**
     * 지정한 오류 코드로 예외를 생성한다.
     *
     * @param commonErrorCode 응답에 사용할 상태·코드·메시지
     */
    public CommonException(CommonErrorCode commonErrorCode) {
        super(commonErrorCode);
    }
}
