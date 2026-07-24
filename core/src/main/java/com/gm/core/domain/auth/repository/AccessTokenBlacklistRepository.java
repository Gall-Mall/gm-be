package com.gm.core.domain.auth.repository;

import java.time.Duration;

/**
 * 로그아웃된 Access Token의 블랙리스트 저장소 계약이다.
 *
 * <p>Access Token 원문 전체가 아니라 JWT 고유 식별자인 jti를 저장한다.</p>
 */
public interface AccessTokenBlacklistRepository {

    /**
     * Access Token의 고유 식별자를 블랙리스트에 등록한다.
     *
     * <p>TTL은 Access Token의 남은 만료 시간으로 설정해야 한다. Access Token이 자연 만료되면 블랙리스트 데이터도 함께 제거된다.</p>
     *
     * @param accessTokenId Access Token의 JWT ID(jti)
     * @param expiration    Access Token의 남은 유효시간
     */
    void save(String accessTokenId, Duration expiration);

    /**
     * 지정한 Access Token이 블랙리스트에 등록되어 있는지 확인한다.
     *
     * @param accessTokenId 확인할 Access Token의 JWT ID(jti)
     * @return 블랙리스트에 등록되어 있으면 {@code true}
     */
    boolean exists(String accessTokenId);
}