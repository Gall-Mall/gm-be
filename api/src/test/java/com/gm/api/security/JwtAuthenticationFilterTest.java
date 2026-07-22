package com.gm.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.user.exception.UserErrorCode;
import com.gm.core.domain.user.exception.UserException;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.service.UserService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserService userService;

    @Mock
    private FilterChain filterChain;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider, userService);

        userId = UUID.randomUUID();

        activeUser = new User(
                "홍길동",
                "홍길동",
                UserStatus.ACTIVE,
                Provider.NAVER,
                "naver-provider-id",
                "01012345678",
                "user@example.com",
                false
        );
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    @DisplayName("유효한 Access Token과 활성 회원이면 SecurityContext에 인증 정보를 등록한다")
    void validAccessTokenAndActiveUser_setsAuthentication() throws Exception {
        // given
        String accessToken = "valid-access-token";
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.validateAndGetUserId(accessToken)).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(activeUser);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isInstanceOf(CustomUserPrincipal.class);

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal.getUser()).isEqualTo(activeUser);
        assertThat(authentication.getDetails()).isNotNull();

        verify(jwtProvider).validateAndGetUserId(accessToken);
        verify(userService).findById(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증하지 않고 다음 필터로 진행한다")
    void noAuthorizationHeader_doesNotAuthenticate() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtProvider, never()).validateAndGetUserId(any(String.class));
        verify(userService, never()).findById(any(UUID.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 형식이 아니면 인증하지 않는다")
    void nonBearerAuthorizationHeader_doesNotAuthenticate() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        request.addHeader("Authorization", "Basic test-token");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtProvider, never()).validateAndGetUserId(any(String.class));
        verify(userService, never()).findById(any(UUID.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 뒤에 토큰이 없으면 인증하지 않는다")
    void emptyBearerToken_doesNotAuthenticate() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        request.addHeader(
                "Authorization",
                "Bearer "
        );

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtProvider, never()).validateAndGetUserId(any(String.class));
        verify(userService, never()).findById(any(UUID.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 검증 중 JwtException이 발생하면 인증하지 않는다")
    void invalidJwt_doesNotAuthenticate() throws Exception {
        // given
        String accessToken = "invalid-access-token";

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.validateAndGetUserId(accessToken)).thenThrow(new JwtException("유효하지 않은 JWT입니다."));

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtProvider).validateAndGetUserId(accessToken);
        verify(userService, never()).findById(any(UUID.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT의 회원 UUID 형식이 잘못되면 인증하지 않는다")
    void invalidUserIdFormat_doesNotAuthenticate() throws Exception {
        // given
        String accessToken = "invalid-user-id-token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.validateAndGetUserId(accessToken))
                .thenThrow(new IllegalArgumentException("잘못된 회원 UUID 형식입니다."));

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtProvider).validateAndGetUserId(accessToken);
        verify(userService, never()).findById(any(UUID.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT에 해당하는 회원이 없으면 인증하지 않는다")
    void userNotFound_doesNotAuthenticate() throws Exception {
        // given
        String accessToken = "valid-access-token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.validateAndGetUserId(accessToken)).thenReturn(userId);
        when(userService.findById(userId)).thenThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtProvider).validateAndGetUserId(accessToken);
        verify(userService).findById(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("비활성 회원이면 SecurityContext에 인증 정보를 등록하지 않는다")
    void inactiveUser_doesNotAuthenticate() throws Exception {
        // given
        String accessToken = "valid-access-token";

        User inactiveUser = new User(
                "홍길동",
                "홍길동",
                UserStatus.WITHDRAWN,
                Provider.NAVER,
                "naver-provider-id",
                "01012345678",
                "user@example.com",
                false
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.validateAndGetUserId(accessToken)).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(inactiveUser);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtProvider).validateAndGetUserId(accessToken);
        verify(userService).findById(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("ONBOARDING 회원이면 인증하지 않는다")
    void onboardingUser_doesNotAuthenticate() throws Exception {
        // given
        String accessToken = "valid-access-token";

        User onboardingUser = new User(
                "홍길동",
                "홍길동",
                UserStatus.ONBOARDING,
                Provider.NAVER,
                "naver-provider-id",
                "01012345678",
                "user@example.com",
                false
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.validateAndGetUserId(accessToken)).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(onboardingUser);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtProvider).validateAndGetUserId(accessToken);
        verify(userService).findById(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("이미 인증 정보가 있으면 JWT 인증을 다시 수행하지 않는다")
    void existingAuthentication_skipsJwtAuthentication() throws Exception {
        // given
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, activeUser);

        Authentication existingAuthentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        request.addHeader("Authorization", "Bearer valid-access-token");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existingAuthentication);

        verify(jwtProvider, never()).validateAndGetUserId(any(String.class));
        verify(userService, never()).findById(any(UUID.class));
        verify(filterChain).doFilter(request, response);
    }
}