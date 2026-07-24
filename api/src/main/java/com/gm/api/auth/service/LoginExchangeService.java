package com.gm.api.auth.service;

import java.time.Duration;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.gm.core.domain.auth.exception.AuthErrorCode;
import com.gm.core.domain.auth.exception.AuthException;
import com.gm.core.domain.auth.model.LoginExchange;
import com.gm.core.domain.auth.repository.LoginExchangeRepository;
import com.gm.core.domain.auth.support.LoginExchangeCodeGenerator;

/**
 * OAuth 로그인 성공 정보를 일회용 교환 코드로 관리한다.
 * <p>교환 코드에는 Access Token이나 Refresh Token 원문을 저장하지 않는다.</p>
 * <p>Redis에는 회원 UUID와 Refresh Token의 jti만 저장한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginExchangeService {

    private final LoginExchangeRepository loginExchangeRepository;
    private final LoginExchangeCodeGenerator loginExchangeCodeGenerator;

    @Value("${auth.login-exchange-expiration:180}")
    private long loginExchangeExpirationSeconds;

    /**
     * 일회용 로그인 교환 코드를 생성하고 Redis에 저장한다.
     *
     * @param userId         서비스 회원 UUID
     * @param refreshTokenId 발급된 Refresh Token의 JWT ID
     * @return 클라이언트에 전달할 일회용 교환 코드
     */
    public String createExchangeCode(UUID userId, String refreshTokenId) {

        validateCreateArguments(userId, refreshTokenId);
        Duration expiration = getLoginExchangeExpiration();
        String exchangeCode = loginExchangeCodeGenerator.generate();
        LoginExchange loginExchange = new LoginExchange(userId, refreshTokenId);

        loginExchangeRepository.save(exchangeCode, loginExchange, expiration);

        log.debug(
                "로그인 교환 코드를 생성했습니다. userId={}, refreshTokenId={}, expirationSeconds={}",
                userId, refreshTokenId, expiration.toSeconds()
        );

        return exchangeCode;
    }

    /**
     * 일회용 로그인 교환 코드를 소비한다.
     * <p>Redis의 consume 연산은 조회와 삭제를 함께 수행하므로, 성공적으로 사용된 교환 코드는 다시 사용할 수 없다.</p>
     *
     * @param exchangeCode 클라이언트가 전달한 교환 코드
     * @return 로그인 교환 정보
     */
    public LoginExchange consume(String exchangeCode) {
        if (!StringUtils.hasText(exchangeCode)) { throw new AuthException(AuthErrorCode.LOGIN_EXCHANGE_CODE_NOT_FOUND); }

        return loginExchangeRepository.consume(exchangeCode)
                .orElseThrow(() -> {
                    log.debug("유효하지 않은 로그인 교환 코드입니다.");

                    return new AuthException(AuthErrorCode.INVALID_LOGIN_EXCHANGE_CODE);
                });
    }

    /**
     * 로그인 교환 정보가 현재 Refresh Token 세션과 연결되어 있는지 확인한다.
     * <p>후속 AuthController에서 쿠키의 Refresh Token jti와 교환 정보의 refreshTokenId를 비교할 때 사용할 수 있다.</p>
     */
    public void validateRefreshTokenSession(LoginExchange loginExchange, String refreshTokenId) {
        if (loginExchange == null) { throw new AuthException(AuthErrorCode.INVALID_LOGIN_EXCHANGE_CODE); }
        if (!StringUtils.hasText(refreshTokenId)) { throw new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND); }
        if (!refreshTokenId.equals(loginExchange.refreshTokenId())) {
            log.debug(
                    "로그인 교환 정보와 Refresh Token 세션이 일치하지 않습니다. "
                            + "exchangeRefreshTokenId={}, requestRefreshTokenId={}",
                    loginExchange.refreshTokenId(), refreshTokenId
            );

            throw new AuthException(AuthErrorCode.LOGIN_EXCHANGE_SESSION_MISMATCH);
        }
    }

    public Duration getLoginExchangeExpiration() {
        if (loginExchangeExpirationSeconds <= 0) {
            throw new IllegalStateException("로그인 교환 코드 만료 시간은 0보다 커야 합니다.");
        }

        return Duration.ofSeconds(loginExchangeExpirationSeconds);
    }

    private void validateCreateArguments(UUID userId, String refreshTokenId) {
        if (userId == null) { throw new IllegalArgumentException("회원 ID는 null일 수 없습니다."); }
        if (!StringUtils.hasText(refreshTokenId)) {
            throw new IllegalArgumentException("Refresh Token ID는 null이거나 공백일 수 없습니다.");
        }
    }
}