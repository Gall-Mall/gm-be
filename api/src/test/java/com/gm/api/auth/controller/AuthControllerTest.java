package com.gm.api.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import com.gm.api.auth.dto.AccessTokenResponse;
import com.gm.api.auth.dto.LoginExchangeRequest;
import com.gm.api.auth.service.AuthTokenService;
import com.gm.api.auth.service.AuthTokenService.IssuedAccessToken;
import com.gm.api.auth.service.LoginExchangeService;
import com.gm.api.auth.service.LogoutService;
import com.gm.api.auth.service.RefreshTokenCookieManager;
import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.auth.exception.AuthErrorCode;
import com.gm.core.domain.auth.exception.AuthException;
import com.gm.core.domain.auth.model.LoginExchange;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

    private static final String EXCHANGE_CODE = "exchange-code";

    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String REFRESH_TOKEN_ID = "refresh-token-id";

    private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 3_600L;

    private static final Duration ACCESS_TOKEN_EXPIRATION =
            Duration.ofHours(1);

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private LoginExchangeService loginExchangeService;

    @Mock
    private LogoutService logoutService;

    @Mock
    private RefreshTokenCookieManager refreshTokenCookieManager;

    @Mock
    private Claims refreshTokenClaims;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(
                jwtProvider,
                authTokenService,
                loginExchangeService,
                logoutService,
                refreshTokenCookieManager
        );
    }

    @Nested
    @DisplayName("OAuth 로그인 Access Token 발급")
    class IssueAccessToken {

        @Test
        @DisplayName("유효한 교환 코드와 Refresh Token이면 Access Token을 발급한다")
        void issuesAccessToken() {
            // given
            UUID userId = UUID.randomUUID();

            LoginExchangeRequest loginExchangeRequest =
                    new LoginExchangeRequest(EXCHANGE_CODE);

            LoginExchange loginExchange =
                    new LoginExchange(
                            userId,
                            REFRESH_TOKEN_ID
                    );

            IssuedAccessToken issuedAccessToken =
                    createIssuedAccessToken();

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

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

            // when
            ResponseEntity<ResponseEnvelope<AccessTokenResponse>> result =
                    authController.issueAccessToken(
                            loginExchangeRequest,
                            request
                    );

            // then
            assertThat(result.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            assertThat(result.getBody()).isNotNull();

            verify(refreshTokenCookieManager)
                    .resolveRefreshToken(request);

            verify(loginExchangeService)
                    .consume(EXCHANGE_CODE);

            verify(jwtProvider)
                    .validateRefreshToken(REFRESH_TOKEN);

            verify(jwtProvider)
                    .getUserId(refreshTokenClaims);

            verify(jwtProvider)
                    .getJti(refreshTokenClaims);

            verify(loginExchangeService)
                    .validateRefreshTokenSession(
                            loginExchange,
                            REFRESH_TOKEN_ID
                    );

            verify(authTokenService)
                    .reissueAccessToken(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("교환 코드는 Refresh Token 검증 전에 소비한다")
        void consumesExchangeCodeBeforeRefreshTokenValidation() {
            // given
            UUID userId = UUID.randomUUID();

            LoginExchangeRequest loginExchangeRequest =
                    new LoginExchangeRequest(EXCHANGE_CODE);

            LoginExchange loginExchange =
                    new LoginExchange(
                            userId,
                            REFRESH_TOKEN_ID
                    );

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

            when(loginExchangeService.consume(EXCHANGE_CODE))
                    .thenReturn(loginExchange);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);

            when(jwtProvider.getUserId(refreshTokenClaims))
                    .thenReturn(userId);

            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);

            when(authTokenService.reissueAccessToken(REFRESH_TOKEN))
                    .thenReturn(createIssuedAccessToken());

            // when
            authController.issueAccessToken(
                    loginExchangeRequest,
                    request
            );

            // then
            InOrder inOrder = Mockito.inOrder(
                    loginExchangeService,
                    jwtProvider,
                    authTokenService
            );

            inOrder.verify(loginExchangeService)
                    .consume(EXCHANGE_CODE);

            inOrder.verify(jwtProvider)
                    .validateRefreshToken(REFRESH_TOKEN);

            inOrder.verify(jwtProvider)
                    .getUserId(refreshTokenClaims);

            inOrder.verify(jwtProvider)
                    .getJti(refreshTokenClaims);

            inOrder.verify(loginExchangeService)
                    .validateRefreshTokenSession(
                            loginExchange,
                            REFRESH_TOKEN_ID
                    );

            inOrder.verify(authTokenService)
                    .reissueAccessToken(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Refresh Token 쿠키가 없으면 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenCookieIsMissing() {
            // given
            LoginExchangeRequest loginExchangeRequest =
                    new LoginExchangeRequest(EXCHANGE_CODE);

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.empty());

            // when & then
            assertAuthException(
                    () -> authController.issueAccessToken(
                            loginExchangeRequest,
                            request
                    ),
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
            );

            verify(refreshTokenCookieManager)
                    .resolveRefreshToken(request);

            verifyNoInteractions(
                    jwtProvider,
                    authTokenService,
                    loginExchangeService,
                    logoutService
            );
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token이면 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenIsInvalid() {
            // given
            UUID userId = UUID.randomUUID();

            LoginExchangeRequest loginExchangeRequest =
                    new LoginExchangeRequest(EXCHANGE_CODE);

            LoginExchange loginExchange =
                    new LoginExchange(
                            userId,
                            REFRESH_TOKEN_ID
                    );

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

            when(loginExchangeService.consume(EXCHANGE_CODE))
                    .thenReturn(loginExchange);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenThrow(new JwtException(
                            "invalid refresh token"
                    ));

            // when & then
            assertAuthException(
                    () -> authController.issueAccessToken(
                            loginExchangeRequest,
                            request
                    ),
                    AuthErrorCode.INVALID_REFRESH_TOKEN
            );

            verify(loginExchangeService)
                    .consume(EXCHANGE_CODE);

            verify(jwtProvider)
                    .validateRefreshToken(REFRESH_TOKEN);

            verify(loginExchangeService, never())
                    .validateRefreshTokenSession(
                            loginExchange,
                            REFRESH_TOKEN_ID
                    );

            verify(authTokenService, never())
                    .reissueAccessToken(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Refresh Token 검증 중 IllegalArgumentException이 발생하면 인증 예외로 변환한다")
        void convertsRefreshTokenIllegalArgumentException() {
            // given
            UUID userId = UUID.randomUUID();

            LoginExchangeRequest loginExchangeRequest =
                    new LoginExchangeRequest(EXCHANGE_CODE);

            LoginExchange loginExchange =
                    new LoginExchange(
                            userId,
                            REFRESH_TOKEN_ID
                    );

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

            when(loginExchangeService.consume(EXCHANGE_CODE))
                    .thenReturn(loginExchange);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenThrow(new IllegalArgumentException(
                            "invalid token argument"
                    ));

            // when & then
            assertAuthException(
                    () -> authController.issueAccessToken(
                            loginExchangeRequest,
                            request
                    ),
                    AuthErrorCode.INVALID_REFRESH_TOKEN
            );

            verify(authTokenService, never())
                    .reissueAccessToken(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("교환 코드 회원과 Refresh Token 회원이 다르면 예외가 발생한다")
        void throwsExceptionWhenExchangeUserDoesNotMatch() {
            // given
            UUID exchangeUserId = UUID.randomUUID();
            UUID refreshTokenUserId = UUID.randomUUID();

            LoginExchangeRequest loginExchangeRequest =
                    new LoginExchangeRequest(EXCHANGE_CODE);

            LoginExchange loginExchange =
                    new LoginExchange(
                            exchangeUserId,
                            REFRESH_TOKEN_ID
                    );

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

            when(loginExchangeService.consume(EXCHANGE_CODE))
                    .thenReturn(loginExchange);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);

            when(jwtProvider.getUserId(refreshTokenClaims))
                    .thenReturn(refreshTokenUserId);

            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);

            // when & then
            assertAuthException(
                    () -> authController.issueAccessToken(
                            loginExchangeRequest,
                            request
                    ),
                    AuthErrorCode.TOKEN_USER_MISMATCH
            );

            verify(loginExchangeService)
                    .validateRefreshTokenSession(
                            loginExchange,
                            REFRESH_TOKEN_ID
                    );

            verify(authTokenService, never())
                    .reissueAccessToken(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Refresh Token 회원 ID가 null이면 회원 불일치 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenUserIdIsNull() {
            // given
            UUID exchangeUserId = UUID.randomUUID();

            LoginExchangeRequest loginExchangeRequest =
                    new LoginExchangeRequest(EXCHANGE_CODE);

            LoginExchange loginExchange =
                    new LoginExchange(
                            exchangeUserId,
                            REFRESH_TOKEN_ID
                    );

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

            when(loginExchangeService.consume(EXCHANGE_CODE))
                    .thenReturn(loginExchange);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);

            when(jwtProvider.getUserId(refreshTokenClaims))
                    .thenReturn(null);

            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);

            // when & then
            assertAuthException(
                    () -> authController.issueAccessToken(
                            loginExchangeRequest,
                            request
                    ),
                    AuthErrorCode.TOKEN_USER_MISMATCH
            );

            verify(authTokenService, never())
                    .reissueAccessToken(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("로그인 교환 정보가 null이면 예외가 발생한다")
        void throwsExceptionWhenLoginExchangeIsNull() {
            // given
            UUID userId = UUID.randomUUID();

            LoginExchangeRequest loginExchangeRequest =
                    new LoginExchangeRequest(EXCHANGE_CODE);

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

            when(loginExchangeService.consume(EXCHANGE_CODE))
                    .thenReturn(null);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);

            when(jwtProvider.getUserId(refreshTokenClaims))
                    .thenReturn(userId);

            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);

            // validateRefreshTokenSession()이 null에 대해 예외를
            // 발생시키도록 실제 서비스 계약을 모킹한다.
            AuthException exception = new AuthException(
                    AuthErrorCode.INVALID_LOGIN_EXCHANGE_CODE
            );

            org.mockito.Mockito.doThrow(exception)
                    .when(loginExchangeService)
                    .validateRefreshTokenSession(
                            null,
                            REFRESH_TOKEN_ID
                    );

            // when & then
            assertAuthException(
                    () -> authController.issueAccessToken(
                            loginExchangeRequest,
                            request
                    ),
                    AuthErrorCode.INVALID_LOGIN_EXCHANGE_CODE
            );

            verify(authTokenService, never())
                    .reissueAccessToken(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Access Token 발급 결과가 null이면 예외가 발생한다")
        void throwsExceptionWhenIssuedAccessTokenIsNull() {
            // given
            UUID userId = UUID.randomUUID();

            LoginExchangeRequest loginExchangeRequest =
                    new LoginExchangeRequest(EXCHANGE_CODE);

            LoginExchange loginExchange =
                    new LoginExchange(
                            userId,
                            REFRESH_TOKEN_ID
                    );

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

            when(loginExchangeService.consume(EXCHANGE_CODE))
                    .thenReturn(loginExchange);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);

            when(jwtProvider.getUserId(refreshTokenClaims))
                    .thenReturn(userId);

            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);

            when(authTokenService.reissueAccessToken(REFRESH_TOKEN))
                    .thenReturn(null);

            // when & then
            assertThatThrownBy(() ->
                    authController.issueAccessToken(
                            loginExchangeRequest,
                            request
                    )
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(
                            "발급된 Access Token 정보는 null일 수 없습니다."
                    );
        }
    }

    @Nested
    @DisplayName("Access Token 재발급")
    class ReissueAccessToken {

        @Test
        @DisplayName("유효한 Refresh Token 쿠키이면 Access Token을 재발급한다")
        void reissuesAccessToken() {
            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            IssuedAccessToken issuedAccessToken =
                    createIssuedAccessToken();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

            when(authTokenService.reissueAccessToken(REFRESH_TOKEN))
                    .thenReturn(issuedAccessToken);

            // when
            ResponseEntity<ResponseEnvelope<AccessTokenResponse>> result =
                    authController.reissueAccessToken(request);

            // then
            assertThat(result.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            assertThat(result.getBody()).isNotNull();

            verify(refreshTokenCookieManager)
                    .resolveRefreshToken(request);

            verify(authTokenService)
                    .reissueAccessToken(REFRESH_TOKEN);

            verifyNoInteractions(
                    jwtProvider,
                    loginExchangeService,
                    logoutService
            );
        }

        @Test
        @DisplayName("Refresh Token 쿠키가 없으면 재발급할 수 없다")
        void throwsExceptionWhenRefreshTokenCookieIsMissing() {
            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.empty());

            // when & then
            assertAuthException(
                    () -> authController.reissueAccessToken(request),
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
            );

            verify(authTokenService, never())
                    .reissueAccessToken(REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Access Token 재발급 결과가 null이면 예외가 발생한다")
        void throwsExceptionWhenReissuedAccessTokenIsNull() {
            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

            when(authTokenService.reissueAccessToken(REFRESH_TOKEN))
                    .thenReturn(null);

            // when & then
            assertThatThrownBy(() ->
                    authController.reissueAccessToken(request)
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(
                            "발급된 Access Token 정보는 null일 수 없습니다."
                    );
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("유효한 Access Token과 Refresh Token이면 로그아웃하고 쿠키를 삭제한다")
        void logsOutCurrentSession() {
            // given
            String authorizationHeader =
                    "Bearer " + ACCESS_TOKEN;

            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

            // when
            ResponseEntity<ResponseEnvelope<Void>> result =
                    authController.logout(
                            authorizationHeader,
                            request,
                            response
                    );

            // then
            assertThat(result.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            assertThat(result.getBody()).isNotNull();

            verify(logoutService)
                    .logout(
                            ACCESS_TOKEN,
                            REFRESH_TOKEN
                    );

            verify(refreshTokenCookieManager)
                    .deleteRefreshTokenCookie(response);
        }

        @Test
        @DisplayName("서버 로그아웃 후 Refresh Token 쿠키를 삭제한다")
        void deletesCookieAfterServerLogout() {
            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

            // when
            authController.logout(
                    "Bearer " + ACCESS_TOKEN,
                    request,
                    response
            );

            // then
            InOrder inOrder = Mockito.inOrder(
                    logoutService,
                    refreshTokenCookieManager
            );

            inOrder.verify(logoutService)
                    .logout(
                            ACCESS_TOKEN,
                            REFRESH_TOKEN
                    );

            inOrder.verify(refreshTokenCookieManager)
                    .deleteRefreshTokenCookie(response);
        }

        @Test
        @DisplayName("Authorization 헤더가 null이면 예외가 발생한다")
        void throwsExceptionWhenAuthorizationHeaderIsNull() {
            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            // when & then
            assertAuthException(
                    () -> authController.logout(
                            null,
                            request,
                            response
                    ),
                    AuthErrorCode.ACCESS_TOKEN_NOT_FOUND
            );

            verifyNoInteractions(
                    logoutService,
                    refreshTokenCookieManager
            );
        }

        @Test
        @DisplayName("Authorization 헤더가 공백이면 예외가 발생한다")
        void throwsExceptionWhenAuthorizationHeaderIsBlank() {
            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            // when & then
            assertAuthException(
                    () -> authController.logout(
                            "   ",
                            request,
                            response
                    ),
                    AuthErrorCode.ACCESS_TOKEN_NOT_FOUND
            );

            verifyNoInteractions(
                    logoutService,
                    refreshTokenCookieManager
            );
        }

        @Test
        @DisplayName("Authorization 헤더가 Bearer 형식이 아니면 예외가 발생한다")
        void throwsExceptionWhenAuthorizationHeaderIsNotBearer() {
            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            // when & then
            assertAuthException(
                    () -> authController.logout(
                            ACCESS_TOKEN,
                            request,
                            response
                    ),
                    AuthErrorCode.INVALID_ACCESS_TOKEN
            );

            verifyNoInteractions(
                    logoutService,
                    refreshTokenCookieManager
            );
        }

        @Test
        @DisplayName("Bearer 뒤에 Access Token이 없으면 예외가 발생한다")
        void throwsExceptionWhenBearerTokenIsMissing() {
            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            // when & then
            assertAuthException(
                    () -> authController.logout(
                            "Bearer ",
                            request,
                            response
                    ),
                    AuthErrorCode.ACCESS_TOKEN_NOT_FOUND
            );

            verifyNoInteractions(
                    logoutService,
                    refreshTokenCookieManager
            );
        }

        @Test
        @DisplayName("Refresh Token 쿠키가 없으면 로그아웃할 수 없다")
        void throwsExceptionWhenRefreshTokenCookieIsMissing() {
            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.empty());

            // when & then
            assertAuthException(
                    () -> authController.logout(
                            "Bearer " + ACCESS_TOKEN,
                            request,
                            response
                    ),
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
            );

            verify(logoutService, never())
                    .logout(
                            ACCESS_TOKEN,
                            REFRESH_TOKEN
                    );

            verify(refreshTokenCookieManager, never())
                    .deleteRefreshTokenCookie(response);
        }

        @Test
        @DisplayName("서버 로그아웃 처리에 실패하면 Refresh Token 쿠키를 삭제하지 않는다")
        void doesNotDeleteCookieWhenLogoutFails() {
            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            when(refreshTokenCookieManager.resolveRefreshToken(request))
                    .thenReturn(Optional.of(REFRESH_TOKEN));

            AuthException logoutException =
                    new AuthException(
                            AuthErrorCode.INVALID_REFRESH_TOKEN
                    );

            org.mockito.Mockito.doThrow(logoutException)
                    .when(logoutService)
                    .logout(
                            ACCESS_TOKEN,
                            REFRESH_TOKEN
                    );

            // when & then
            assertAuthException(
                    () -> authController.logout(
                            "Bearer " + ACCESS_TOKEN,
                            request,
                            response
                    ),
                    AuthErrorCode.INVALID_REFRESH_TOKEN
            );

            verify(refreshTokenCookieManager, never())
                    .deleteRefreshTokenCookie(response);
        }
    }

    private IssuedAccessToken createIssuedAccessToken() {
        return new IssuedAccessToken(
                ACCESS_TOKEN,
                ACCESS_TOKEN_EXPIRATION_SECONDS
        );
    }

    private void assertAuthException(
            ThrowingAction action,
            AuthErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        AuthException.class,
                        exception -> assertThat(
                                exception.getErrorCode()
                        ).isEqualTo(expectedErrorCode)
                );
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run();
    }
}