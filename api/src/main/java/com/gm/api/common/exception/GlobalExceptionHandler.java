package com.gm.api.common.exception;

import java.time.Instant;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.core.exception.BusinessException;
import com.gm.core.exception.CommonErrorCode;
import com.gm.core.exception.ErrorCode;

/**
 * Controller 경계까지 전파된 예외를 공통 HTTP 응답으로 변환한다.
 *
 * <p>예상 가능한 비즈니스 예외는 지정된 오류 계약을 사용하고,
 * 예상하지 못한 예외는 내부 상세를 숨긴 공통 500 응답으로 변환한다.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외를 오류 코드에 지정된 상태와 공통 실패 응답으로 변환한다.
     *
     * @param exception 처리할 비즈니스 예외
     * @return 오류 코드의 HTTP 상태와 실패 응답
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseEnvelope<Void>> handleBusinessException(
            BusinessException exception
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        if (errorCode.getStatus() >= 500) {
            log.error("[{}] {}", errorCode.getCode(), exception.getMessage(), exception);
        } else {
            log.warn("[{}] {}", errorCode.getCode(), exception.getMessage());
        }

        return ResponseEntity.status(errorCode.getStatus())
                .body(ResponseEnvelope.fail(errorCode));
    }

    /**
     * 요청 값 검증 실패를 공통 입력값 오류(COMMON-002) 응답으로 변환한다.
     *
     * <p>DTO 검증 실패, 필수 헤더 누락을 포함한다.</p>
     *
     * @param exception 요청 값 검증 관련 예외
     * @return 400 상태의 공통 실패 응답
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MissingRequestHeaderException.class
    })
    public ResponseEntity<ResponseEnvelope<Void>> handleInvalidInput(Exception exception) {
        ErrorCode errorCode = CommonErrorCode.INVALID_INPUT;
        log.warn("[{}] {}", errorCode.getCode(), exception.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ResponseEnvelope.fail(errorCode));
    }

    /**
     * 요청 형식 오류를 공통 형식 오류(COMMON-003) 응답으로 변환한다.
     *
     * <p>JSON 파싱 실패, UUID·파라미터 타입 변환 실패를 포함한다.</p>
     *
     * @param exception 요청 형식 관련 예외
     * @return 400 상태의 공통 실패 응답
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ResponseEnvelope<Void>> handleInvalidFormat(Exception exception) {
        ErrorCode errorCode = CommonErrorCode.INVALID_FORMAT;
        log.warn("[{}] {}", errorCode.getCode(), exception.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ResponseEnvelope.fail(errorCode));
    }

    /**
     * 별도로 처리되지 않은 예외를 내부 정보가 노출되지 않는 500 응답으로 변환한다.
     *
     * @param exception 처리되지 않은 예외
     * @return 공통 서버 오류 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseEnvelope<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        ErrorCode errorCode = CommonErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ResponseEnvelope.fail(errorCode));
    }
}
