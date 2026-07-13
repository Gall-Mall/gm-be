package com.gm.core.exception;

/**
 * 특정 도메인에 속하지 않는 공통 오류 코드를 정의한다.
 *
 * <p>예상하지 못한 서버 예외는 내부 상세를 노출하지 않고
 * {@link #INTERNAL_ERROR}의 고정 메시지로 응답한다.</p>
 */
public enum CommonErrorCode implements ErrorCode {

    /** 처리되지 않은 서버 예외에 사용하는 공통 오류이다. */
    INTERNAL_ERROR(500, "COMMON-001", "서버 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;

    /**
     * 공통 오류 코드를 생성한다.
     *
     * @param status HTTP 상태 코드
     * @param code 외부에 노출할 안정적인 오류 코드
     * @param message 외부에 노출 가능한 기본 메시지
     */
    CommonErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    /** {@inheritDoc} */
    @Override
    public int getStatus() {
        return status;
    }

    /** {@inheritDoc} */
    @Override
    public String getCode() {
        return code;
    }

    /** {@inheritDoc} */
    @Override
    public String getMessage() {
        return message;
    }
}
