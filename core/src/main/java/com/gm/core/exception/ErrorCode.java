package com.gm.core.exception;

/**
 * 애플리케이션 오류를 외부 응답으로 변환하기 위한 공통 계약이다.
 *
 * <p>HTTP 프레임워크 타입을 직접 사용하지 않고 정수 상태값을 제공해
 * core 모듈이 Web 계층에 의존하지 않도록 한다.</p>
 */
public interface ErrorCode {

    /**
     * 오류에 대응하는 HTTP 상태 코드를 반환한다.
     *
     * @return HTTP 상태 코드
     */
    int getStatus();

    /**
     * 클라이언트와 로그에서 안정적으로 식별할 오류 코드를 반환한다.
     *
     * @return 오류 코드
     */
    String getCode();

    /**
     * 클라이언트에 노출 가능한 기본 오류 메시지를 반환한다.
     *
     * @return 사용자용 오류 메시지
     */
    String getMessage();
}
