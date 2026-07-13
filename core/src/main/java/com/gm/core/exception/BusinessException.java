package com.gm.core.exception;

/**
 * 예상 가능한 비즈니스 정책 위반을 표현하는 공통 예외 기반 클래스이다.
 *
 * <p>하위 예외는 {@link ErrorCode}를 통해 외부 응답에 필요한 상태·코드·메시지를 제공한다.
 * Web 계층은 이 예외를 받아 HTTP 응답으로 변환한다.</p>
 */
public abstract class BusinessException extends RuntimeException {

    /** 외부 응답 변환에 사용할 오류 계약이다. */
    private final ErrorCode errorCode;

    /**
     * 지정한 오류 코드를 사용하는 비즈니스 예외를 생성한다.
     *
     * @param errorCode 비즈니스 오류의 상태·코드·메시지
     */
    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 응답 변환에 사용할 오류 코드를 반환한다.
     *
     * @return 이 예외에 연결된 오류 코드
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
