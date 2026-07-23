package com.gm.api.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserResult;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private DefaultOAuth2UserService delegate;

    @Mock
    private UserService userService;

    @Mock
    private OAuth2UserRequest request;

    @Mock
    private OAuth2User oauth2User;

    @Test
    @DisplayName("네이버 사용자 정보로 회원을 조회하거나 생성하고 Principal을 반환한다")
    void loadsNaverUserAndReturnsPrincipal() {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUser(UserStatus.ONBOARDING);

        Map<String, Object> attributes = Map.of(
                "response", Map.of(
                        "id", "naver-provider-id",
                        "name", "홍길동",
                        "email", "user@example.com",
                        "mobile", "010-1234-5678"
                )
        );

        when(delegate.loadUser(request)).thenReturn(oauth2User);

        when(oauth2User.getAttributes()).thenReturn(attributes);

        when(userService.findOrCreateWithId(
                "홍길동",
                Provider.NAVER,
                "naver-provider-id",
                "010-1234-5678",
                "user@example.com"
        )).thenReturn(UserResult.of(userId, user));

        CustomOAuth2UserService service = new CustomOAuth2UserService(delegate, userService);

        // when
        OAuth2User result = service.loadUser(request);

        // then
        assertThat(result).isInstanceOf(CustomUserPrincipal.class);

        CustomUserPrincipal principal = (CustomUserPrincipal) result;

        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal.getUser()).isEqualTo(user);
        assertThat(principal.getAttributes()).isEqualTo(attributes);
        assertThat(principal.getName()).isEqualTo(userId.toString());

        verify(userService).findOrCreateWithId(
                "홍길동",
                Provider.NAVER,
                "naver-provider-id",
                "010-1234-5678",
                "user@example.com"
        );
    }

    @Test
    @DisplayName("네이버 응답에 response가 없으면 회원 저장을 시도하지 않고 예외가 발생한다")
    void throwsWhenNaverResponseDoesNotExist() {
        // given
        when(delegate.loadUser(request)).thenReturn(oauth2User);

        when(oauth2User.getAttributes()).thenReturn(Map.of("message", "success"));

        CustomOAuth2UserService service = new CustomOAuth2UserService(delegate, userService);

        // when & then
        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("네이버 사용자 정보(response)가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("네이버 사용자 식별자가 없으면 회원 저장을 시도하지 않고 예외가 발생한다")
    void throwsWhenNaverProviderIdDoesNotExist() {
        // given
        Map<String, Object> attributes = Map.of(
                "response", Map.of(
                        "name", "홍길동",
                        "email", "user@example.com",
                        "mobile", "010-1234-5678"
                )
        );

        when(delegate.loadUser(request)).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(attributes);

        CustomOAuth2UserService service = new CustomOAuth2UserService(delegate, userService);

        // when & then
        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("네이버 사용자 식별자가 존재하지 않습니다.");
    }

    private User createUser(UserStatus status) {
        return new User(
                "홍길동",
                "홍길동",
                status,
                Provider.NAVER,
                "naver-provider-id",
                "010-1234-5678",
                "user@example.com",
                false
        );
    }
}