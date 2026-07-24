package com.gm.api.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import com.gm.api.auth.service.AuthTokenService.IssuedAccessToken;
import com.gm.api.auth.service.AuthTokenService.IssuedRefreshToken;
import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.auth.exception.AuthErrorCode;
import com.gm.core.domain.auth.exception.AuthException;
import com.gm.core.domain.auth.repository.RefreshTokenRepository;
import com.gm.core.domain.user.exception.UserErrorCode;
import com.gm.core.domain.user.exception.UserException;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.service.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthTokenService 단위 테스트")
class AuthTokenServiceTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String REFRESH_TOKEN_ID = "refresh-token-jti";

    private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 3_600L;
    private static final Duration REFRESH_TOKEN_EXPIRATION =
            Duration.ofDays(14);

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private Claims refreshTokenClaims;

    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        authTokenService = new AuthTokenService(
                jwtProvider,
                userService,
                refreshTokenRepository
        );
    }

    @Nested
    @DisplayName("Refresh Token 신규 발급")
    class IssueRefreshToken {

        @Test
        @DisplayName("ACTIVE 회원이면 Refresh Token을 발급하고 Redis에 저장한다")
        void issuesRefreshTokenForActiveUser() {
            // given
            UUID userId = UUID.randomUUID();
            User user = createUser(UserStatus.ACTIVE);

            when(userService.findById(userId)).thenReturn(user);
            when(jwtProvider.createRefreshToken(userId))
                    .thenReturn(REFRESH_TOKEN);
            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);
            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);
            when(jwtProvider.getRefreshTokenExpiration())
                    .thenReturn(REFRESH_TOKEN_EXPIRATION);

            // when
            IssuedRefreshToken result =
                    authTokenService.issueRefreshToken(userId);

            // then
            assertThat(result.token()).isEqualTo(REFRESH_TOKEN);
            assertThat(result.refreshTokenId())
                    .isEqualTo(REFRESH_TOKEN_ID);
            assertThat(result.expiration())
                    .isEqualTo(REFRESH_TOKEN_EXPIRATION);

            verify(userService).findById(userId);
            verify(jwtProvider).createRefreshToken(userId);
            verify(jwtProvider)
                    .validateRefreshToken(REFRESH_TOKEN);
            verify(jwtProvider).getJti(refreshTokenClaims);
            verify(jwtProvider).getRefreshTokenExpiration();

            verify(refreshTokenRepository).save(
                    REFRESH_TOKEN_ID,
                    userId,
                    REFRESH_TOKEN,
                    REFRESH_TOKEN_EXPIRATION
            );
        }

        @Test
        @DisplayName("ONBOARDING 회원도 Refresh Token을 발급할 수 있다")
        void issuesRefreshTokenForOnboardingUser() {
            // given
            UUID userId = UUID.randomUUID();
            User user = createUser(UserStatus.ONBOARDING);

            when(userService.findById(userId)).thenReturn(user);
            when(jwtProvider.createRefreshToken(userId))
                    .thenReturn(REFRESH_TOKEN);
            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);
            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);
            when(jwtProvider.getRefreshTokenExpiration())
                    .thenReturn(REFRESH_TOKEN_EXPIRATION);

            // when
            IssuedRefreshToken result =
                    authTokenService.issueRefreshToken(userId);

            // then
            assertThat(result.token()).isEqualTo(REFRESH_TOKEN);

            verify(refreshTokenRepository).save(
                    REFRESH_TOKEN_ID,
                    userId,
                    REFRESH_TOKEN,
                    REFRESH_TOKEN_EXPIRATION
            );
        }

        @Test
        @DisplayName("회원 ID가 null이면 예외가 발생한다")
        void throwsExceptionWhenUserIdIsNull() {
            assertThatThrownBy(() ->
                    authTokenService.issueRefreshToken(null)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("회원 ID는 null일 수 없습니다.");

            verifyNoInteractions(
                    userService,
                    jwtProvider,
                    refreshTokenRepository
            );
        }

        @Test
        @DisplayName("회원을 찾을 수 없으면 인증 회원 없음 예외가 발생한다")
        void throwsExceptionWhenUserDoesNotExist() {
            // given
            UUID userId = UUID.randomUUID();

            when(userService.findById(userId))
                    .thenThrow(new UserException(
                            UserErrorCode.USER_NOT_FOUND
                    ));

            // when & then
            assertAuthException(
                    () -> authTokenService.issueRefreshToken(userId),
                    AuthErrorCode.AUTHENTICATED_USER_NOT_FOUND
            );

            verify(jwtProvider, never())
                    .createRefreshToken(userId);
            verifyNoInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("탈퇴 회원이면 Refresh Token을 발급하지 않는다")
        void throwsExceptionForWithdrawnUser() {
            // given
            UUID userId = UUID.randomUUID();
            User user = createUser(UserStatus.WITHDRAWN);

            when(userService.findById(userId)).thenReturn(user);

            // when & then
            assertAuthException(
                    () -> authTokenService.issueRefreshToken(userId),
                    AuthErrorCode.WITHDRAWN_USER
            );

            verify(jwtProvider, never())
                    .createRefreshToken(userId);
            verifyNoInteractions(refreshTokenRepository);
        }
    }

    @Nested
    @DisplayName("Access Token 신규 발급")
    class IssueAccessToken {

        @Test
        @DisplayName("ACTIVE 회원이면 현재 회원 상태를 포함한 Access Token을 발급한다")
        void issuesAccessTokenForActiveUser() {
            // given
            UUID userId = UUID.randomUUID();
            User user = createUser(UserStatus.ACTIVE);

            when(userService.findById(userId)).thenReturn(user);
            when(jwtProvider.createAccessToken(
                    userId,
                    UserStatus.ACTIVE
            )).thenReturn(ACCESS_TOKEN);
            when(jwtProvider.getAccessTokenExpirationSeconds())
                    .thenReturn(ACCESS_TOKEN_EXPIRATION_SECONDS);

            // when
            IssuedAccessToken result =
                    authTokenService.issueAccessToken(userId);

            // then
            assertThat(result.token()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.expiresIn())
                    .isEqualTo(ACCESS_TOKEN_EXPIRATION_SECONDS);

            verify(jwtProvider).createAccessToken(
                    userId,
                    UserStatus.ACTIVE
            );
        }

        @Test
        @DisplayName("ONBOARDING 회원도 Access Token을 발급할 수 있다")
        void issuesAccessTokenForOnboardingUser() {
            // given
            UUID userId = UUID.randomUUID();
            User user = createUser(UserStatus.ONBOARDING);

            when(userService.findById(userId)).thenReturn(user);
            when(jwtProvider.createAccessToken(
                    userId,
                    UserStatus.ONBOARDING
            )).thenReturn(ACCESS_TOKEN);
            when(jwtProvider.getAccessTokenExpirationSeconds())
                    .thenReturn(ACCESS_TOKEN_EXPIRATION_SECONDS);

            // when
            IssuedAccessToken result =
                    authTokenService.issueAccessToken(userId);

            // then
            assertThat(result.token()).isEqualTo(ACCESS_TOKEN);

            verify(jwtProvider).createAccessToken(
                    userId,
                    UserStatus.ONBOARDING
            );
        }

        @Test
        @DisplayName("회원 ID가 null이면 예외가 발생한다")
        void throwsExceptionWhenUserIdIsNull() {
            assertThatThrownBy(() ->
                    authTokenService.issueAccessToken(null)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("회원 ID는 null일 수 없습니다.");

            verifyNoInteractions(
                    userService,
                    jwtProvider,
                    refreshTokenRepository
            );
        }

        @Test
        @DisplayName("회원을 찾을 수 없으면 인증 회원 없음 예외가 발생한다")
        void throwsExceptionWhenUserDoesNotExist() {
            // given
            UUID userId = UUID.randomUUID();

            when(userService.findById(userId))
                    .thenThrow(new UserException(
                            UserErrorCode.USER_NOT_FOUND
                    ));

            // when & then
            assertAuthException(
                    () -> authTokenService.issueAccessToken(userId),
                    AuthErrorCode.AUTHENTICATED_USER_NOT_FOUND
            );

            verify(jwtProvider, never())
                    .createAccessToken(
                            userId,
                            UserStatus.ACTIVE
                    );
        }

        @Test
        @DisplayName("탈퇴 회원이면 Access Token을 발급하지 않는다")
        void throwsExceptionForWithdrawnUser() {
            // given
            UUID userId = UUID.randomUUID();
            User user = createUser(UserStatus.WITHDRAWN);

            when(userService.findById(userId)).thenReturn(user);

            // when & then
            assertAuthException(
                    () -> authTokenService.issueAccessToken(userId),
                    AuthErrorCode.WITHDRAWN_USER
            );

            verify(jwtProvider, never())
                    .createAccessToken(
                            userId,
                            UserStatus.WITHDRAWN
                    );
        }
    }

    @Nested
    @DisplayName("Access Token 재발급")
    class ReissueAccessToken {

        @Test
        @DisplayName("유효한 Refresh Token이면 새 Access Token을 발급한다")
        void reissuesAccessTokenWithValidRefreshToken() {
            // given
            UUID userId = UUID.randomUUID();
            User user = createUser(UserStatus.ACTIVE);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);
            when(jwtProvider.getUserId(refreshTokenClaims))
                    .thenReturn(userId);
            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);

            when(refreshTokenRepository.matches(
                    REFRESH_TOKEN_ID,
                    userId,
                    REFRESH_TOKEN
            )).thenReturn(true);

            when(userService.findById(userId)).thenReturn(user);
            when(jwtProvider.createAccessToken(
                    userId,
                    UserStatus.ACTIVE
            )).thenReturn(ACCESS_TOKEN);
            when(jwtProvider.getAccessTokenExpirationSeconds())
                    .thenReturn(ACCESS_TOKEN_EXPIRATION_SECONDS);

            // when
            IssuedAccessToken result =
                    authTokenService.reissueAccessToken(REFRESH_TOKEN);

            // then
            assertThat(result.token()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.expiresIn())
                    .isEqualTo(ACCESS_TOKEN_EXPIRATION_SECONDS);

            verify(refreshTokenRepository).matches(
                    REFRESH_TOKEN_ID,
                    userId,
                    REFRESH_TOKEN
            );
            verify(jwtProvider).createAccessToken(
                    userId,
                    UserStatus.ACTIVE
            );
        }

        @Test
        @DisplayName("Refresh Token이 null이면 존재하지 않음 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenIsNull() {
            assertAuthException(
                    () -> authTokenService.reissueAccessToken(null),
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
            );

            verifyNoInteractions(
                    jwtProvider,
                    userService,
                    refreshTokenRepository
            );
        }

        @Test
        @DisplayName("Refresh Token이 공백이면 존재하지 않음 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenIsBlank() {
            assertAuthException(
                    () -> authTokenService.reissueAccessToken("   "),
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
            );

            verifyNoInteractions(
                    jwtProvider,
                    userService,
                    refreshTokenRepository
            );
        }

        @Test
        @DisplayName("JWT 검증에 실패하면 유효하지 않은 Refresh Token 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenJwtIsInvalid() {
            // given
            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenThrow(new JwtException("invalid token"));

            // when & then
            assertAuthException(
                    () -> authTokenService
                            .reissueAccessToken(REFRESH_TOKEN),
                    AuthErrorCode.INVALID_REFRESH_TOKEN
            );

            verifyNoInteractions(
                    userService,
                    refreshTokenRepository
            );
        }

        @Test
        @DisplayName("Redis 세션과 일치하지 않으면 재발급에 실패한다")
        void throwsExceptionWhenRedisSessionDoesNotMatch() {
            // given
            UUID userId = UUID.randomUUID();

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);
            when(jwtProvider.getUserId(refreshTokenClaims))
                    .thenReturn(userId);
            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);
            when(refreshTokenRepository.matches(
                    REFRESH_TOKEN_ID,
                    userId,
                    REFRESH_TOKEN
            )).thenReturn(false);

            // when & then
            assertAuthException(
                    () -> authTokenService
                            .reissueAccessToken(REFRESH_TOKEN),
                    AuthErrorCode.INVALID_REFRESH_TOKEN
            );

            verifyNoInteractions(userService);

            verify(jwtProvider, never())
                    .createAccessToken(
                            userId,
                            UserStatus.ACTIVE
                    );
        }

        @Test
        @DisplayName("Redis 검증 후 회원을 찾을 수 없으면 인증 회원 없음 예외가 발생한다")
        void throwsExceptionWhenUserDoesNotExist() {
            // given
            UUID userId = UUID.randomUUID();

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);
            when(jwtProvider.getUserId(refreshTokenClaims))
                    .thenReturn(userId);
            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);
            when(refreshTokenRepository.matches(
                    REFRESH_TOKEN_ID,
                    userId,
                    REFRESH_TOKEN
            )).thenReturn(true);

            when(userService.findById(userId))
                    .thenThrow(new UserException(
                            UserErrorCode.USER_NOT_FOUND
                    ));

            // when & then
            assertAuthException(
                    () -> authTokenService
                            .reissueAccessToken(REFRESH_TOKEN),
                    AuthErrorCode.AUTHENTICATED_USER_NOT_FOUND
            );

            verify(jwtProvider, never())
                    .createAccessToken(
                            userId,
                            UserStatus.ACTIVE
                    );
        }

        @Test
        @DisplayName("탈퇴 회원의 Refresh Token으로 재발급할 수 없다")
        void throwsExceptionForWithdrawnUser() {
            // given
            UUID userId = UUID.randomUUID();
            User withdrawnUser =
                    createUser(UserStatus.WITHDRAWN);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshTokenClaims);
            when(jwtProvider.getUserId(refreshTokenClaims))
                    .thenReturn(userId);
            when(jwtProvider.getJti(refreshTokenClaims))
                    .thenReturn(REFRESH_TOKEN_ID);
            when(refreshTokenRepository.matches(
                    REFRESH_TOKEN_ID,
                    userId,
                    REFRESH_TOKEN
            )).thenReturn(true);
            when(userService.findById(userId))
                    .thenReturn(withdrawnUser);

            // when & then
            assertAuthException(
                    () -> authTokenService
                            .reissueAccessToken(REFRESH_TOKEN),
                    AuthErrorCode.WITHDRAWN_USER
            );

            verify(jwtProvider, never())
                    .createAccessToken(
                            userId,
                            UserStatus.WITHDRAWN
                    );
        }
    }

    @Nested
    @DisplayName("발급 결과 모델")
    class IssuedTokenRecord {

        @Test
        @DisplayName("Access Token이 공백이면 발급 결과를 생성할 수 없다")
        void rejectsBlankAccessToken() {
            assertThatThrownBy(() ->
                    new IssuedAccessToken(
                            " ",
                            ACCESS_TOKEN_EXPIRATION_SECONDS
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Access Token은 null이거나 공백일 수 없습니다."
                    );
        }

        @Test
        @DisplayName("Access Token 만료 시간이 0 이하면 발급 결과를 생성할 수 없다")
        void rejectsInvalidAccessTokenExpiration() {
            assertThatThrownBy(() ->
                    new IssuedAccessToken(ACCESS_TOKEN, 0)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Access Token 만료 시간은 0보다 커야 합니다."
                    );
        }

        @Test
        @DisplayName("Refresh Token ID가 공백이면 발급 결과를 생성할 수 없다")
        void rejectsBlankRefreshTokenId() {
            assertThatThrownBy(() ->
                    new IssuedRefreshToken(
                            REFRESH_TOKEN,
                            " ",
                            REFRESH_TOKEN_EXPIRATION
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token ID는 null이거나 공백일 수 없습니다."
                    );
        }

        @Test
        @DisplayName("Refresh Token 만료 시간이 유효하지 않으면 발급 결과를 생성할 수 없다")
        void rejectsInvalidRefreshTokenExpiration() {
            assertThatThrownBy(() ->
                    new IssuedRefreshToken(
                            REFRESH_TOKEN,
                            REFRESH_TOKEN_ID,
                            Duration.ZERO
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token 만료 시간은 0보다 커야 합니다."
                    );
        }
    }

    private User createUser(UserStatus status) {
        return new User(
                "테스트 사용자",
                "테스트 닉네임",
                status,
                Provider.NAVER,
                "naver-provider-id",
                "010-1234-5678",
                "test@example.com",
                true,
                null,
                null,
                null
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