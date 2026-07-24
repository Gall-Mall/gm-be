package com.gm.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
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
import com.gm.core.domain.auth.repository.AccessTokenBlacklistRepository;
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
    private static final String ACCESS_TOKEN_ID = "access-token-jti";

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserService userService;

    @Mock
    private AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    @Mock
    private Claims claims;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        filter = new JwtAuthenticationFilter(
                jwtProvider,
                userService,
                accessTokenBlacklistRepository
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

        when(jwtProvider.validateAccessToken(ACCESS_TOKEN))
                .thenReturn(claims);
        when(jwtProvider.getJti(claims))
                .thenReturn(ACCESS_TOKEN_ID);
        when(accessTokenBlacklistRepository.exists(ACCESS_TOKEN_ID))
                .thenReturn(false);
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

        verify(accessTokenBlacklistRepository)
                .exists(ACCESS_TOKEN_ID);
    }

    @Test
    @DisplayName("ONBOARDING 회원도 인증 정보를 등록한다")
    void authenticatesOnboardingUser() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUser(UserStatus.ONBOARDING);

        when(jwtProvider.validateAccessToken(ACCESS_TOKEN))
                .thenReturn(claims);
        when(jwtProvider.getJti(claims))
                .thenReturn(ACCESS_TOKEN_ID);
        when(accessTokenBlacklistRepository.exists(ACCESS_TOKEN_ID))
                .thenReturn(false);
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

        when(jwtProvider.validateAccessToken(ACCESS_TOKEN))
                .thenReturn(claims);
        when(jwtProvider.getJti(claims))
                .thenReturn(ACCESS_TOKEN_ID);
        when(accessTokenBlacklistRepository.exists(ACCESS_TOKEN_ID))
                .thenReturn(false);
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
    @DisplayName("블랙리스트에 등록된 Access Token이면 인증 정보를 등록하지 않는다")
    void doesNotAuthenticateBlacklistedToken() throws Exception {
        // given
        when(jwtProvider.validateAccessToken(ACCESS_TOKEN))
                .thenReturn(claims);
        when(jwtProvider.getJti(claims))
                .thenReturn(ACCESS_TOKEN_ID);
        when(accessTokenBlacklistRepository.exists(ACCESS_TOKEN_ID))
                .thenReturn(true);

        // when
        executeFilter(ACCESS_TOKEN);

        // then
        assertThat(
                SecurityContextHolder.getContext().getAuthentication()
        ).isNull();

        verify(accessTokenBlacklistRepository)
                .exists(ACCESS_TOKEN_ID);
        verify(jwtProvider, never())
                .getUserId(claims);
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

        verify(jwtProvider)
                .validate(ACCESS_TOKEN);
        verify(accessTokenBlacklistRepository, never())
                .exists(ACCESS_TOKEN_ID);
    }

    @Test
    @DisplayName("JWT의 회원이 존재하지 않으면 인증 정보를 등록하지 않는다")
    void doesNotAuthenticateMissingUser() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        when(jwtProvider.validateAccessToken(ACCESS_TOKEN))
                .thenReturn(claims);
        when(jwtProvider.getJti(claims))
                .thenReturn(ACCESS_TOKEN_ID);
        when(accessTokenBlacklistRepository.exists(ACCESS_TOKEN_ID))
                .thenReturn(false);
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

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증을 시도하지 않는다")
    void doesNotAuthenticateWithoutAuthorizationHeader() throws Exception {
        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod("GET");
        request.setRequestURI("/api/users/me");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(
                SecurityContextHolder.getContext().getAuthentication()
        ).isNull();

        verify(jwtProvider, never())
                .validate(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("Bearer 형식이 아닌 Authorization 헤더이면 인증을 시도하지 않는다")
    void doesNotAuthenticateWithInvalidAuthorizationScheme() throws Exception {
        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod("GET");
        request.setRequestURI("/api/users/me");
        request.addHeader(
                "Authorization",
                "Basic " + ACCESS_TOKEN
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(
                SecurityContextHolder.getContext().getAuthentication()
        ).isNull();

        verify(jwtProvider, never())
                .validate(ACCESS_TOKEN);
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
                false,
                null,
                null,
                null
        );
    }
}