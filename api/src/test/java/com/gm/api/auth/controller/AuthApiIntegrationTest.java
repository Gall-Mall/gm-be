package com.gm.api.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gm.api.auth.service.AuthTokenService;
import com.gm.api.auth.service.AuthTokenService.IssuedAccessToken;
import com.gm.api.auth.service.LoginExchangeService;
import com.gm.api.auth.service.LogoutService;
import com.gm.api.auth.service.RefreshTokenCookieManager;
import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.auth.exception.AuthErrorCode;
import com.gm.core.domain.auth.exception.AuthException;
import com.gm.core.domain.auth.model.LoginExchange;
import com.gm.core.exception.CommonErrorCode;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("인증 API 통합 테스트")
class AuthApiIntegrationTest {

    private static final String EXCHANGE_CODE = "exchange-code";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String REFRESH_TOKEN_ID = "refresh-token-id";
    private static final String ACCESS_TOKEN = "access-token";

    private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 3_600L;

    @Autowired
    private MockMvc mockMvc;

    /*
     * 실제 Controller, DTO 검증, JSON 변환, GlobalExceptionHandler,
     * SecurityFilterChain은 Spring Context에서 사용한다.
     *
     * JWT·Redis·서비스 계층은 이미 단위 테스트를 완료했으므로
     * 이 테스트에서는 Mock Bean으로 교체한다.
     */
    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private LoginExchangeService loginExchangeService;

    @MockitoBean
    private LogoutService logoutService;

    @MockitoBean
    private RefreshTokenCookieManager refreshTokenCookieManager;

    private UUID userId;
    private Claims refreshTokenClaims;
    private LoginExchange loginExchange;
    private IssuedAccessToken issuedAccessToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        refreshTokenClaims = mock(Claims.class);

        loginExchange = new LoginExchange(
                userId,
                REFRESH_TOKEN_ID
        );

        issuedAccessToken = new IssuedAccessToken(
                ACCESS_TOKEN,
                ACCESS_TOKEN_EXPIRATION_SECONDS
        );
    }

    @Nested
    @DisplayName("POST /api/auth/token")
    class IssueAccessToken {

        @Test
        @DisplayName("유효한 교환 코드와 Refresh Token이면 Access Token을 반환한다")
        void issueAccessTokenSucceeds() throws Exception {
            // given
            mockRefreshTokenCookie();

            when(loginExchangeService.consume(EXCHANGE_CODE))
                    .thenReturn(loginExchange);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);

            when(jwtProvider.getUserId(refreshTokenClaims))
                    .thenReturn(userId);

            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);

            when(authTokenService.reissueAccessToken(REFRESH_TOKEN))
                    .thenReturn(issuedAccessToken);

            String requestBody = """
                    {
                      "code": "exchange-code"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("성공했습니다."))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.accessToken").value(ACCESS_TOKEN))
                    .andExpect(jsonPath("$.data.expiresIn")
                            .value(ACCESS_TOKEN_EXPIRATION_SECONDS));

            verify(loginExchangeService)
                    .consume(EXCHANGE_CODE);

            verify(loginExchangeService)
                    .validateRefreshTokenSession(
                            loginExchange,
                            REFRESH_TOKEN_ID
                    );

            verify(authTokenService)
                    .reissueAccessToken(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("교환 코드가 공백이면 COMMON-002를 반환한다")
        void blankExchangeCodeReturnsInvalidInput() throws Exception {
            String requestBody = """
                    {
                      "code": " "
                    }
                    """;

            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code")
                            .value(CommonErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(loginExchangeService, never())
                    .consume(any());
        }

        @Test
        @DisplayName("교환 코드 필드가 없으면 COMMON-002를 반환한다")
        void missingExchangeCodeReturnsInvalidInput() throws Exception {
            String requestBody = """
                    {
                    }
                    """;

            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code")
                            .value(CommonErrorCode.INVALID_INPUT.getCode()));

            verify(loginExchangeService, never())
                    .consume(any());
        }

        @Test
        @DisplayName("잘못된 JSON이면 COMMON-005를 반환한다")
        void malformedJsonReturnsInvalidFormat() throws Exception {
            String malformedBody = """
                    {
                      "code": "exchange-code"
                    """;

            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code")
                            .value(CommonErrorCode.INVALID_FORMAT.getCode()));

            verify(loginExchangeService, never())
                    .consume(any());
        }

        @Test
        @DisplayName("요청 본문이 없으면 COMMON-005를 반환한다")
        void missingRequestBodyReturnsInvalidFormat() throws Exception {
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code")
                            .value(CommonErrorCode.INVALID_FORMAT.getCode()));

            verify(loginExchangeService, never())
                    .consume(any());
        }

        @Test
        @DisplayName("Refresh Token 쿠키가 없으면 REFRESH_TOKEN_NOT_FOUND를 반환한다")
        void missingRefreshTokenReturnsError() throws Exception {
            when(refreshTokenCookieManager.resolveRefreshToken(
                    any(HttpServletRequest.class)
            )).thenReturn(Optional.empty());

            String requestBody = """
                    {
                      "code": "exchange-code"
                    }
                    """;

            AuthErrorCode errorCode =
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND;

            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().is(errorCode.getStatus()))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code")
                            .value(errorCode.getCode()))
                    .andExpect(jsonPath("$.message")
                            .value(errorCode.getMessage()));

            verify(loginExchangeService, never())
                    .consume(any());
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token이면 INVALID_REFRESH_TOKEN을 반환한다")
        void invalidRefreshTokenReturnsError() throws Exception {
            // given
            mockRefreshTokenCookie();

            when(loginExchangeService.consume(EXCHANGE_CODE))
                    .thenReturn(loginExchange);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenThrow(new JwtException(
                            "invalid refresh token"
                    ));

            String requestBody = """
                    {
                      "code": "exchange-code"
                    }
                    """;

            AuthErrorCode errorCode =
                    AuthErrorCode.INVALID_REFRESH_TOKEN;

            // when & then
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().is(errorCode.getStatus()))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code")
                            .value(errorCode.getCode()));

            verify(loginExchangeService)
                    .consume(EXCHANGE_CODE);

            verify(authTokenService, never())
                    .reissueAccessToken(any());
        }

        @Test
        @DisplayName("교환 코드 사용자와 Refresh Token 사용자가 다르면 TOKEN_USER_MISMATCH를 반환한다")
        void differentUserReturnsTokenUserMismatch() throws Exception {
            // given
            UUID differentUserId = UUID.randomUUID();

            mockRefreshTokenCookie();

            when(loginExchangeService.consume(EXCHANGE_CODE))
                    .thenReturn(loginExchange);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);

            when(jwtProvider.getUserId(refreshTokenClaims))
                    .thenReturn(differentUserId);

            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);

            String requestBody = """
                    {
                      "code": "exchange-code"
                    }
                    """;

            AuthErrorCode errorCode =
                    AuthErrorCode.TOKEN_USER_MISMATCH;

            // when & then
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().is(errorCode.getStatus()))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code")
                            .value(errorCode.getCode()));

            verify(loginExchangeService)
                    .validateRefreshTokenSession(
                            loginExchange,
                            REFRESH_TOKEN_ID
                    );

            verify(authTokenService, never())
                    .reissueAccessToken(any());
        }

        @Test
        @DisplayName("이미 사용한 교환 코드를 다시 사용하면 INVALID_LOGIN_EXCHANGE_CODE를 반환한다")
        void reusedExchangeCodeReturnsError() throws Exception {
            // given
            mockRefreshTokenCookie();

            when(loginExchangeService.consume(EXCHANGE_CODE))
                    .thenReturn(loginExchange)
                    .thenThrow(new AuthException(
                            AuthErrorCode.INVALID_LOGIN_EXCHANGE_CODE
                    ));

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);

            when(jwtProvider.getUserId(refreshTokenClaims))
                    .thenReturn(userId);

            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);

            when(authTokenService.reissueAccessToken(REFRESH_TOKEN))
                    .thenReturn(issuedAccessToken);

            String requestBody = """
                    {
                      "code": "exchange-code"
                    }
                    """;

            // 첫 번째 요청은 성공한다.
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            AuthErrorCode errorCode =
                    AuthErrorCode.INVALID_LOGIN_EXCHANGE_CODE;

            // 동일한 교환 코드의 두 번째 요청은 실패한다.
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().is(errorCode.getStatus()))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code")
                            .value(errorCode.getCode()));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/token/refresh")
    class ReissueAccessToken {

        @Test
        @DisplayName("유효한 Refresh Token이면 새로운 Access Token을 반환한다")
        void reissueAccessTokenSucceeds() throws Exception {
            // given
            mockRefreshTokenCookie();

            when(authTokenService.reissueAccessToken(REFRESH_TOKEN))
                    .thenReturn(issuedAccessToken);

            // when & then
            mockMvc.perform(post("/api/auth/token/refresh"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.accessToken").value(ACCESS_TOKEN))
                    .andExpect(jsonPath("$.data.expiresIn")
                            .value(ACCESS_TOKEN_EXPIRATION_SECONDS));

            verify(authTokenService)
                    .reissueAccessToken(REFRESH_TOKEN);

            verify(loginExchangeService, never())
                    .consume(any());
        }

        @Test
        @DisplayName("Refresh Token 쿠키가 없으면 REFRESH_TOKEN_NOT_FOUND를 반환한다")
        void missingRefreshTokenReturnsError() throws Exception {
            when(refreshTokenCookieManager.resolveRefreshToken(
                    any(HttpServletRequest.class)
            )).thenReturn(Optional.empty());

            AuthErrorCode errorCode =
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND;

            mockMvc.perform(post("/api/auth/token/refresh"))
                    .andExpect(status().is(errorCode.getStatus()))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code")
                            .value(errorCode.getCode()));

            verify(authTokenService, never())
                    .reissueAccessToken(any());
        }

        @Test
        @DisplayName("서비스에서 Refresh Token 검증에 실패하면 INVALID_REFRESH_TOKEN을 반환한다")
        void invalidRefreshTokenReturnsError() throws Exception {
            // given
            mockRefreshTokenCookie();

            when(authTokenService.reissueAccessToken(REFRESH_TOKEN))
                    .thenThrow(new AuthException(
                            AuthErrorCode.INVALID_REFRESH_TOKEN
                    ));

            AuthErrorCode errorCode =
                    AuthErrorCode.INVALID_REFRESH_TOKEN;

            // when & then
            mockMvc.perform(post("/api/auth/token/refresh"))
                    .andExpect(status().is(errorCode.getStatus()))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code")
                            .value(errorCode.getCode()));
        }
    }

    private void mockRefreshTokenCookie() {
        when(refreshTokenCookieManager.resolveRefreshToken(
                any(HttpServletRequest.class)
        )).thenReturn(Optional.of(REFRESH_TOKEN));
    }
}
