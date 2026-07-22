package com.gm.api.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserResult;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private DefaultOAuth2UserService defaultOAuth2UserService;

    @Mock
    private UserService userService;
    private CustomOAuth2UserService customOAuth2UserService;

    @BeforeEach
    void setUp() {
        customOAuth2UserService =
                new CustomOAuth2UserService(defaultOAuth2UserService, userService);
    }

    @Test
    @DisplayName("네이버 사용자 정보로 회원을 조회 또는 생성하고 CustomUserPrincipal을 반환한다")
    void loadUser_success() {
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

        Map<String, Object> responseAttributes = Map.of(
                "id", "naver-provider-id",
                "name", "홍길동",
                "email", "user@example.com",
                "mobile", "010-1234-5678"
        );

        Map<String, Object> attributes = Map.of("response", responseAttributes);

        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "response");

        OAuth2UserRequest userRequest = createOAuth2UserRequest();

        when(defaultOAuth2UserService.loadUser(userRequest)).thenReturn(oauth2User);

        when(userService.findOrCreateWithId(
                "홍길동",
                Provider.NAVER,
                "naver-provider-id",
                "010-1234-5678",
                "user@example.com"
        )).thenReturn(UserResult.of(userId, user));

        // when
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        // then
        assertThat(result).isInstanceOf(CustomUserPrincipal.class);

        CustomUserPrincipal principal = (CustomUserPrincipal) result;

        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal.getUser()).isEqualTo(user);
        assertThat(principal.getAttributes()).isEqualTo(attributes);

        verify(userService).findOrCreateWithId(
                "홍길동",
                Provider.NAVER,
                "naver-provider-id",
                "010-1234-5678",
                "user@example.com"
        );
    }

    private OAuth2UserRequest createOAuth2UserRequest() {
        ClientRegistration clientRegistration =
                ClientRegistration
                        .withRegistrationId("naver")
                        .clientId("test-client-id")
                        .clientSecret("test-client-secret")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("http://localhost/api/auth/oauth/naver/callback")
                        .scope(Set.of("name", "email", "mobile"))
                        .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                        .tokenUri("https://nid.naver.com/oauth2.0/token")
                        .userInfoUri("https://openapi.naver.com/v1/nid/me")
                        .userNameAttributeName("response")
                        .clientName("Naver")
                        .build();

        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "naver-access-token",
                        null,
                        null
                );

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }
}