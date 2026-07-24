package com.gm.core.domain.auth.support;

import java.util.Base64;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * OAuth 로그인 결과를 교환할 때 사용하는
 * 추측하기 어려운 일회성 코드를 생성한다.
 */
@Component
public class LoginExchangeCodeGenerator {

    /**
     * 32바이트의 난수를 사용한다.
     *
     * <p>Base64 URL-safe 인코딩 후 padding을 제거하므로 생성되는 문자열은 URL 쿼리 파라미터에 안전하게 포함할 수 있다.</p>
     */
    private static final int RANDOM_BYTE_LENGTH = 32;

    private final SecureRandom secureRandom;

    public LoginExchangeCodeGenerator() { this.secureRandom = new SecureRandom(); }

    /**
     * 새로운 일회성 로그인 교환 코드를 생성한다.
     *
     * @return URL-safe Base64 형식의 로그인 교환 코드
     */
    public String generate() {
        byte[] randomBytes = new byte[RANDOM_BYTE_LENGTH];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}