package com.gm.core.domain.auth.repository;

import java.time.Duration;
import java.util.Optional;

import com.gm.core.domain.auth.model.LoginExchange;

/**
 * OAuth 로그인 성공 결과를 일회성 교환 코드와 연결하여 임시 저장하는 저장소 계약이다.
 */
public interface LoginExchangeRepository {

    /**
     * 일회성 로그인 교환 정보를 저장한다.
     *
     * @param exchangeCode 일회성 로그인 교환 코드
     * @param loginExchange 저장할 로그인 교환 정보
     * @param expiration Redis에 적용할 짧은 만료 시간
     */
    void save(String exchangeCode, LoginExchange loginExchange, Duration expiration);

    /**
     * 일회성 로그인 교환 정보를 조회하고 즉시 삭제한다.
     *
     * <p>정상적으로 반환된 교환 코드는 다시 사용할 수 없어야 한다. Redis 구현체에서는 조회와 삭제가 원자적으로 수행되어야 한다.</p>
     *
     * @param exchangeCode 프런트엔드가 전달한 일회성 교환 코드
     * @return 유효한 교환 정보, 없거나 만료되었으면 빈 Optional
     */
    Optional<LoginExchange> consume(String exchangeCode);
}