package com.gm.api.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.io.Writer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;

import tools.jackson.databind.ObjectMapper;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.core.domain.auth.exception.AuthErrorCode;

@ExtendWith(MockitoExtension.class)
class OAuth2FailureHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("OAuth2 로그인 실패 시 AUTH-004 공통 실패 응답을 반환한다")
    void returnsCommonFailureResponse() throws Exception {
        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod("GET");
        request.setRequestURI("/api/auth/oauth/naver/callback");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        AuthenticationServiceException exception =
                new AuthenticationServiceException("OAuth2 failure");

        OAuth2FailureHandler handler =
                new OAuth2FailureHandler(objectMapper);

        // when
        handler.onAuthenticationFailure(
                request,
                response,
                exception
        );

        // then
        assertThat(response.getStatus())
                .isEqualTo(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED.getStatus());

        assertThat(response.getContentType())
                .startsWith("application/json");

        assertThat(response.getCharacterEncoding())
                .isEqualTo("UTF-8");

        assertThat(response.getHeader("Cache-Control"))
                .isEqualTo("no-store");

        assertThat(response.getHeader("Pragma"))
                .isEqualTo("no-cache");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ResponseEnvelope<Void>> captor =
                ArgumentCaptor.forClass(ResponseEnvelope.class);

        verify(objectMapper).writeValue(
                any(Writer.class),
                captor.capture()
        );

        ResponseEnvelope<Void> body = captor.getValue();

        assertThat(body.success()).isFalse();
        assertThat(body.code()).isEqualTo("AUTH-004");
        assertThat(body.message()).isEqualTo("소셜 로그인에 실패했습니다.");
        assertThat(body.data()).isNull();
    }
}