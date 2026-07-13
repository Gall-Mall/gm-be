package com.gm.api.common.response;

import java.time.Instant;

import com.gm.core.exception.ErrorCode;

/**
 * JSON API의 성공·실패 응답을 동일한 최상위 구조로 제공한다.
 *
 * @param success 요청 성공 여부
 * @param code 클라이언트가 분기에 사용할 응답 코드
 * @param message 사용자에게 제공할 응답 메시지
 * @param data 성공 시 반환할 실제 응답 데이터
 * @param <T> 실제 응답 데이터 타입
 */
public record ResponseEnvelope<T>(
        boolean success,
        String code,
        String message,
        T data
) {

    private static final String SUCCESS_CODE = "SUCCESS";
    private static final String SUCCESS_MESSAGE = "성공했습니다.";

    /**
     * 실제 DTO를 공통 성공 응답으로 감싼다.
     *
     * @param data 반환할 실제 응답 데이터
     * @param <T> 응답 데이터 타입
     * @return 성공 상태와 공통 코드가 포함된 응답
     */
    public static <T> ResponseEnvelope<T> success(T data) {
        return new ResponseEnvelope<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /**
     * 오류 코드를 공통 실패 응답으로 변환한다.
     *
     * <p>실패 응답은 성공 응답과 같은 JSON 구조를 유지하며 {@code data}는 {@code null}이다.</p>
     *
     * @param errorCode 실패 상태·코드·메시지를 제공하는 오류 코드
     * @return 데이터가 없는 공통 실패 응답
     */
    public static ResponseEnvelope<Void> fail(ErrorCode errorCode) {
        return new ResponseEnvelope<>(
                false,
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }
}
