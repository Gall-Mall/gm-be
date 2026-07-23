package com.gm.core.exception;

import lombok.Getter;

/**
 * 특정 도메인에 속하지 않는 공통 오류 코드를 정의한다.
 *
 * <p>예상하지 못한 서버 예외는 내부 상세를 노출하지 않고
 * {@link #INTERNAL_ERROR}의 고정 메시지로 응답한다.</p>
 */
@Getter
public enum CommonErrorCode implements ErrorCode {

    /** 처리되지 않은 서버 예외에 사용하는 공통 오류이다. */
    INTERNAL_ERROR(500, "COMMON-001", "서버 오류가 발생했습니다."),
    /** 요청 DTO 검증에 실패한 경우 사용하는 공통 오류이다. */
    INVALID_INPUT(400, "COMMON-002", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(401, "COMMON-003", "인증이 필요합니다."),
    ACCESS_DENIED(403, "COMMON-004", "해당 요청을 수행할 권한이 없습니다."),
    /** JSON 파싱, UUID 변환 또는 필수 파라미터 변환에 실패한 경우 사용하는 공통 오류이다. */
    INVALID_FORMAT(400, "COMMON-005", "요청 형식이 올바르지 않습니다."),
    /** 짧은 시간 내 동일 사용자의 요청 횟수가 허용 한도를 초과한 경우 사용하는 공통 오류이다. */
    RATE_LIMIT_EXCEEDED(429, "COMMON-006", "요청이 너무 많습니다.");

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
}
