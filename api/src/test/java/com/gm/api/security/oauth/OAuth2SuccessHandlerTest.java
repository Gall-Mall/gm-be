package com.gm.api.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import com.gm.api.auth.service.AuthTokenService;
import com.gm.api.auth.service.LoginExchangeService;
import com.gm.api.auth.service.RefreshTokenCookieManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    private static final String SUCCESS_REDIRECT_URI = "http://localhost:8080/oauth/success";

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private RefreshTokenCookieManager refreshTokenCookieManager;

    @Mock
    private LoginExchangeService loginExchangeService;

    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(
                SUCCESS_REDIRECT_URI,
                authTokenService,
                loginExchangeService,
                refreshTokenCookieManager
        );
    }

    @Test
    @DisplayName("네이버 OAuth2 로그인 성공 시 설정된 주소로 리다이렉트한다")
    void redirectsAfterOAuth2Success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        User user = new User(
                "홍길동",
                "홍길동",
                UserStatus.ONBOARDING,
                Provider.NAVER,
                "naver-provider-id",
                "010-1234-5678",
                "user@example.com",
                false,
                null,
                null,
                null
        );

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, user);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        AuthTokenService.IssuedRefreshToken issuedRefreshToken =
                new AuthTokenService.IssuedRefreshToken(
                        "refresh-token",
                        "refresh-token-id",
                        Duration.ofDays(14)
                );

        when(authTokenService.issueRefreshToken(userId))
                .thenReturn(issuedRefreshToken);

        when(loginExchangeService.createExchangeCode(
                userId,
                "refresh-token-id"
        )).thenReturn("exchange-code");

        // when
        handler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        // then
        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl())
                .isEqualTo(
                        SUCCESS_REDIRECT_URI + "?code=exchange-code"
                );

        verify(authTokenService)
                .issueRefreshToken(userId);

        verify(refreshTokenCookieManager)
                .addRefreshTokenCookie(
                        response,
                        "refresh-token"
                );

        verify(loginExchangeService)
                .createExchangeCode(
                        userId,
                        "refresh-token-id"
                );

        assertThat(response.getHeader("Cache-Control"))
                .isEqualTo("no-store");

        assertThat(response.getHeader("Pragma"))
                .isEqualTo("no-cache");
    }
}