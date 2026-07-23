package com.gm.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.user.exception.UserErrorCode;
import com.gm.core.domain.user.exception.UserException;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.service.UserService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String ACCESS_TOKEN = "access-token";

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserService userService;

    @Mock
    private Claims claims;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        filter = new JwtAuthenticationFilter(
                jwtProvider,
                userService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("정상 JWT와 ACTIVE 회원이면 인증 정보를 등록한다")
    void authenticatesActiveUser() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUser(UserStatus.ACTIVE);

        when(jwtProvider.validate(ACCESS_TOKEN))
                .thenReturn(claims);

        when(jwtProvider.getUserId(claims))
                .thenReturn(userId);

        when(userService.findById(userId))
                .thenReturn(user);

        // when
        executeFilter(ACCESS_TOKEN);

        // then
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal())
                .isInstanceOf(CustomUserPrincipal.class);

        CustomUserPrincipal principal =
                (CustomUserPrincipal) authentication.getPrincipal();

        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("ONBOARDING 회원도 인증 정보를 등록한다")
    void authenticatesOnboardingUser() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUser(UserStatus.ONBOARDING);

        when(jwtProvider.validate(ACCESS_TOKEN))
                .thenReturn(claims);

        when(jwtProvider.getUserId(claims))
                .thenReturn(userId);

        when(userService.findById(userId))
                .thenReturn(user);

        // when
        executeFilter(ACCESS_TOKEN);

        // then
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();

        CustomUserPrincipal principal =
                (CustomUserPrincipal) authentication.getPrincipal();

        assertThat(principal.getUser().status())
                .isEqualTo(UserStatus.ONBOARDING);
    }

    @Test
    @DisplayName("WITHDRAWN 회원이면 인증 정보를 등록하지 않는다")
    void doesNotAuthenticateWithdrawnUser() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUser(UserStatus.WITHDRAWN);

        when(jwtProvider.validate(ACCESS_TOKEN))
                .thenReturn(claims);

        when(jwtProvider.getUserId(claims))
                .thenReturn(userId);

        when(userService.findById(userId))
                .thenReturn(user);

        // when
        executeFilter(ACCESS_TOKEN);

        // then
        assertThat(
                SecurityContextHolder.getContext().getAuthentication()
        ).isNull();
    }

    @Test
    @DisplayName("만료되거나 유효하지 않은 JWT이면 인증 정보를 등록하지 않는다")
    void doesNotAuthenticateInvalidToken() throws Exception {
        // given
        when(jwtProvider.validate(ACCESS_TOKEN))
                .thenThrow(new JwtException("Invalid token"));

        // when
        executeFilter(ACCESS_TOKEN);

        // then
        assertThat(
                SecurityContextHolder.getContext().getAuthentication()
        ).isNull();

        verify(jwtProvider).validate(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("JWT의 회원이 존재하지 않으면 인증 정보를 등록하지 않는다")
    void doesNotAuthenticateMissingUser() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        when(jwtProvider.validate(ACCESS_TOKEN))
                .thenReturn(claims);

        when(jwtProvider.getUserId(claims))
                .thenReturn(userId);

        when(userService.findById(userId))
                .thenThrow(new UserException(UserErrorCode.USER_NOT_FOUND));

        // when
        executeFilter(ACCESS_TOKEN);

        // then
        assertThat(
                SecurityContextHolder.getContext().getAuthentication()
        ).isNull();
    }

    private void executeFilter(String token) throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        request.addHeader(
                "Authorization",
                "Bearer " + token
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                filterChain
        );
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
                status == UserStatus.ACTIVE
        );
    }
}