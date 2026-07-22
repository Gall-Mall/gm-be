package com.gm.api.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import com.gm.api.auth.dto.TokenResponse;
import com.gm.api.auth.service.AuthTokenService;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private AuthTokenService authTokenService;

    private ObjectMapper objectMapper;
    private OAuth2SuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        successHandler = new OAuth2SuccessHandler(authTokenService, objectMapper);
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 시 Access Token을 JSON으로 반환한다")
    void onAuthenticationSuccess_returnsTokenResponse() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        User user = new User(
                "홍길동",
                "홍길동",
                UserStatus.ACTIVE,
                Provider.NAVER,
                "naver-provider-id",
                "01012345678",
                "user@example.com",
                false
        );

        CustomUserPrincipal principal = new CustomUserPrincipal(
                userId,
                user,
                Map.of(
                        "response",
                        Map.of(
                                "id", "naver-provider-id",
                                "name", "홍길동",
                                "email", "user@example.com",
                                "mobile", "010-1234-5678"))
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        TokenResponse tokenResponse = TokenResponse.of("test-access-token");

        when(authTokenService.issue(userId)).thenReturn(tokenResponse);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");

        TokenResponse result = objectMapper.readValue(response.getContentAsString(), TokenResponse.class);

        assertThat(result.accessToken()).isEqualTo("test-access-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");

        verify(authTokenService).issue(userId);
    }
}