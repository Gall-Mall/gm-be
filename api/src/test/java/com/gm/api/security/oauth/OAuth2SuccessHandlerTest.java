package com.gm.api.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;

class OAuth2SuccessHandlerTest {

    private static final String SUCCESS_REDIRECT_URI =
            "http://localhost:8080/oauth/success";

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
                false
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

        OAuth2SuccessHandler handler =
                new OAuth2SuccessHandler(SUCCESS_REDIRECT_URI);

        // when
        handler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        // then
        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl())
                .isEqualTo(SUCCESS_REDIRECT_URI);

        assertThat(response.getHeader("Cache-Control"))
                .isEqualTo("no-store");

        assertThat(response.getHeader("Pragma"))
                .isEqualTo("no-cache");
    }
}