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

import com.gm.api.security.jwt.JwtProvider;
import com.gm.core.domain.auth.exception.AuthErrorCode;
import com.gm.core.domain.auth.exception.AuthException;
import com.gm.core.domain.auth.repository.AccessTokenBlacklistRepository;
import com.gm.core.domain.auth.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutService 단위 테스트")
class LogoutServiceTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    private static final String ACCESS_TOKEN_ID = "access-token-id";
    private static final String REFRESH_TOKEN_ID = "refresh-token-id";

    private static final Duration REMAINING_EXPIRATION =
            Duration.ofMinutes(30);

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AccessTokenBlacklistRepository
            accessTokenBlacklistRepository;

    @Mock
    private Claims accessClaims;

    @Mock
    private Claims refreshClaims;

    private LogoutService logoutService;

    @BeforeEach
    void setUp() {
        logoutService = new LogoutService(
                jwtProvider,
                refreshTokenRepository,
                accessTokenBlacklistRepository
        );
    }

    @Nested
    @DisplayName("정상 로그아웃")
    class LogoutSuccess {

        @Test
        @DisplayName("유효한 토큰과 세션이면 Access Token을 블랙리스트에 등록하고 Refresh Token을 삭제한다")
        void logsOutCurrentSession() {
            // given
            UUID userId = UUID.randomUUID();

            prepareValidTokenClaims(userId, userId);

            when(refreshTokenRepository.matches(
                    REFRESH_TOKEN_ID,
                    userId,
                    REFRESH_TOKEN
            )).thenReturn(true);

            when(accessTokenBlacklistRepository.exists(
                    ACCESS_TOKEN_ID
            )).thenReturn(false);

            when(jwtProvider.getRemainingExpiration(accessClaims))
                    .thenReturn(REMAINING_EXPIRATION);

            // when
            logoutService.logout(
                    ACCESS_TOKEN,
                    REFRESH_TOKEN
            );

            // then
            verify(jwtProvider)
                    .validateAccessToken(ACCESS_TOKEN);

            verify(jwtProvider)
                    .validateRefreshToken(REFRESH_TOKEN);

            verify(jwtProvider)
                    .getUserId(accessClaims);

            verify(jwtProvider)
                    .getUserId(refreshClaims);

            verify(jwtProvider)
                    .getJti(accessClaims);

            verify(jwtProvider)
                    .getJti(refreshClaims);

            verify(refreshTokenRepository)
                    .matches(
                            REFRESH_TOKEN_ID,
                            userId,
                            REFRESH_TOKEN
                    );

            verify(accessTokenBlacklistRepository)
                    .exists(ACCESS_TOKEN_ID);

            verify(jwtProvider)
                    .getRemainingExpiration(accessClaims);

            verify(accessTokenBlacklistRepository)
                    .save(
                            ACCESS_TOKEN_ID,
                            REMAINING_EXPIRATION
                    );

            verify(refreshTokenRepository)
                    .delete(REFRESH_TOKEN_ID);
        }

        @Test
        @DisplayName("Access Token이 이미 블랙리스트에 있으면 중복 저장하지 않고 Refresh Token만 삭제한다")
        void doesNotSaveDuplicateBlacklistEntry() {
            // given
            UUID userId = UUID.randomUUID();

            prepareValidTokenClaims(userId, userId);

            when(refreshTokenRepository.matches(
                    REFRESH_TOKEN_ID,
                    userId,
                    REFRESH_TOKEN
            )).thenReturn(true);

            when(accessTokenBlacklistRepository.exists(
                    ACCESS_TOKEN_ID
            )).thenReturn(true);

            // when
            logoutService.logout(
                    ACCESS_TOKEN,
                    REFRESH_TOKEN
            );

            // then
            verify(accessTokenBlacklistRepository)
                    .exists(ACCESS_TOKEN_ID);

            verify(jwtProvider, never())
                    .getRemainingExpiration(accessClaims);

            verify(accessTokenBlacklistRepository, never())
                    .save(
                            ACCESS_TOKEN_ID,
                            REMAINING_EXPIRATION
                    );

            verify(refreshTokenRepository)
                    .delete(REFRESH_TOKEN_ID);
        }
    }

    @Nested
    @DisplayName("Access Token 필수값 검증")
    class AccessTokenRequired {

        @Test
        @DisplayName("Access Token이 null이면 예외가 발생한다")
        void throwsExceptionWhenAccessTokenIsNull() {
            assertAuthException(
                    () -> logoutService.logout(
                            null,
                            REFRESH_TOKEN
                    ),
                    AuthErrorCode.ACCESS_TOKEN_NOT_FOUND
            );

            verifyNoInteractions(
                    jwtProvider,
                    refreshTokenRepository,
                    accessTokenBlacklistRepository
            );
        }

        @Test
        @DisplayName("Access Token이 빈 문자열이면 예외가 발생한다")
        void throwsExceptionWhenAccessTokenIsEmpty() {
            assertAuthException(
                    () -> logoutService.logout(
                            "",
                            REFRESH_TOKEN
                    ),
                    AuthErrorCode.ACCESS_TOKEN_NOT_FOUND
            );

            verifyNoInteractions(
                    jwtProvider,
                    refreshTokenRepository,
                    accessTokenBlacklistRepository
            );
        }

        @Test
        @DisplayName("Access Token이 공백이면 예외가 발생한다")
        void throwsExceptionWhenAccessTokenIsBlank() {
            assertAuthException(
                    () -> logoutService.logout(
                            "   ",
                            REFRESH_TOKEN
                    ),
                    AuthErrorCode.ACCESS_TOKEN_NOT_FOUND
            );

            verifyNoInteractions(
                    jwtProvider,
                    refreshTokenRepository,
                    accessTokenBlacklistRepository
            );
        }
    }

    @Nested
    @DisplayName("Refresh Token 필수값 검증")
    class RefreshTokenRequired {

        @Test
        @DisplayName("Refresh Token이 null이면 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenIsNull() {
            assertAuthException(
                    () -> logoutService.logout(
                            ACCESS_TOKEN,
                            null
                    ),
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
            );

            verifyNoInteractions(
                    jwtProvider,
                    refreshTokenRepository,
                    accessTokenBlacklistRepository
            );
        }

        @Test
        @DisplayName("Refresh Token이 빈 문자열이면 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenIsEmpty() {
            assertAuthException(
                    () -> logoutService.logout(
                            ACCESS_TOKEN,
                            ""
                    ),
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
            );

            verifyNoInteractions(
                    jwtProvider,
                    refreshTokenRepository,
                    accessTokenBlacklistRepository
            );
        }

        @Test
        @DisplayName("Refresh Token이 공백이면 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenIsBlank() {
            assertAuthException(
                    () -> logoutService.logout(
                            ACCESS_TOKEN,
                            "   "
                    ),
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
            );

            verifyNoInteractions(
                    jwtProvider,
                    refreshTokenRepository,
                    accessTokenBlacklistRepository
            );
        }
    }

    @Nested
    @DisplayName("Access Token JWT 검증")
    class AccessTokenValidation {

        @Test
        @DisplayName("유효하지 않은 Access Token이면 예외가 발생한다")
        void throwsExceptionWhenAccessTokenIsInvalid() {
            // given
            when(jwtProvider.validateAccessToken(ACCESS_TOKEN))
                    .thenThrow(new JwtException(
                            "invalid access token"
                    ));

            // when & then
            assertAuthException(
                    () -> logoutService.logout(
                            ACCESS_TOKEN,
                            REFRESH_TOKEN
                    ),
                    AuthErrorCode.INVALID_ACCESS_TOKEN
            );

            verify(jwtProvider)
                    .validateAccessToken(ACCESS_TOKEN);

            verify(jwtProvider, never())
                    .validateRefreshToken(REFRESH_TOKEN);

            verifyNoInteractions(
                    refreshTokenRepository,
                    accessTokenBlacklistRepository
            );
        }

        @Test
        @DisplayName("Access Token 검증 중 IllegalArgumentException이 발생하면 인증 예외로 변환한다")
        void convertsAccessTokenIllegalArgumentException() {
            // given
            when(jwtProvider.validateAccessToken(ACCESS_TOKEN))
                    .thenThrow(new IllegalArgumentException(
                            "invalid token argument"
                    ));

            // when & then
            assertAuthException(
                    () -> logoutService.logout(
                            ACCESS_TOKEN,
                            REFRESH_TOKEN
                    ),
                    AuthErrorCode.INVALID_ACCESS_TOKEN
            );

            verify(jwtProvider)
                    .validateAccessToken(ACCESS_TOKEN);

            verify(jwtProvider, never())
                    .validateRefreshToken(REFRESH_TOKEN);

            verifyNoInteractions(
                    refreshTokenRepository,
                    accessTokenBlacklistRepository
            );
        }
    }

    @Nested
    @DisplayName("Refresh Token JWT 검증")
    class RefreshTokenValidation {

        @Test
        @DisplayName("유효하지 않은 Refresh Token이면 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenIsInvalid() {
            // given
            when(jwtProvider.validateAccessToken(ACCESS_TOKEN))
                    .thenReturn(accessClaims);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenThrow(new JwtException(
                            "invalid refresh token"
                    ));

            // when & then
            assertAuthException(
                    () -> logoutService.logout(
                            ACCESS_TOKEN,
                            REFRESH_TOKEN
                    ),
                    AuthErrorCode.INVALID_REFRESH_TOKEN
            );

            verify(jwtProvider)
                    .validateAccessToken(ACCESS_TOKEN);

            verify(jwtProvider)
                    .validateRefreshToken(REFRESH_TOKEN);

            verify(jwtProvider, never())
                    .getUserId(accessClaims);

            verifyNoInteractions(
                    refreshTokenRepository,
                    accessTokenBlacklistRepository
            );
        }

        @Test
        @DisplayName("Refresh Token 검증 중 IllegalArgumentException이 발생하면 인증 예외로 변환한다")
        void convertsRefreshTokenIllegalArgumentException() {
            // given
            when(jwtProvider.validateAccessToken(ACCESS_TOKEN))
                    .thenReturn(accessClaims);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenThrow(new IllegalArgumentException(
                            "invalid token argument"
                    ));

            // when & then
            assertAuthException(
                    () -> logoutService.logout(
                            ACCESS_TOKEN,
                            REFRESH_TOKEN
                    ),
                    AuthErrorCode.INVALID_REFRESH_TOKEN
            );

            verify(jwtProvider)
                    .validateAccessToken(ACCESS_TOKEN);

            verify(jwtProvider)
                    .validateRefreshToken(REFRESH_TOKEN);

            verifyNoInteractions(
                    refreshTokenRepository,
                    accessTokenBlacklistRepository
            );
        }
    }

    @Nested
    @DisplayName("토큰 회원 일치 검증")
    class TokenUserValidation {

        @Test
        @DisplayName("Access Token과 Refresh Token의 회원이 다르면 예외가 발생한다")
        void throwsExceptionWhenTokenUsersDoNotMatch() {
            // given
            UUID accessTokenUserId = UUID.randomUUID();
            UUID refreshTokenUserId = UUID.randomUUID();

            when(jwtProvider.validateAccessToken(ACCESS_TOKEN))
                    .thenReturn(accessClaims);

            when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                    .thenReturn(refreshClaims);

            when(jwtProvider.getUserId(accessClaims))
                    .thenReturn(accessTokenUserId);

            when(jwtProvider.getUserId(refreshClaims))
                    .thenReturn(refreshTokenUserId);

            // when & then
            assertAuthException(
                    () -> logoutService.logout(
                            ACCESS_TOKEN,
                            REFRESH_TOKEN
                    ),
                    AuthErrorCode.TOKEN_USER_MISMATCH
            );

            verify(jwtProvider)
                    .getUserId(accessClaims);

            verify(jwtProvider)
                    .getUserId(refreshClaims);

            verify(jwtProvider, never())
                    .getJti(accessClaims);

            verify(jwtProvider, never())
                    .getJti(refreshClaims);

            verifyNoInteractions(
                    refreshTokenRepository,
                    accessTokenBlacklistRepository
            );
        }
    }

    @Nested
    @DisplayName("Refresh Token Redis 세션 검증")
    class RefreshTokenSessionValidation {

        @Test
        @DisplayName("Redis의 현재 Refresh Token 세션과 일치하지 않으면 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenSessionDoesNotMatch() {
            // given
            UUID userId = UUID.randomUUID();

            prepareValidTokenClaims(userId, userId);

            when(refreshTokenRepository.matches(
                    REFRESH_TOKEN_ID,
                    userId,
                    REFRESH_TOKEN
            )).thenReturn(false);

            // when & then
            assertAuthException(
                    () -> logoutService.logout(
                            ACCESS_TOKEN,
                            REFRESH_TOKEN
                    ),
                    AuthErrorCode.INVALID_REFRESH_TOKEN
            );

            verify(refreshTokenRepository)
                    .matches(
                            REFRESH_TOKEN_ID,
                            userId,
                            REFRESH_TOKEN
                    );

            verifyNoInteractions(
                    accessTokenBlacklistRepository
            );

            verify(refreshTokenRepository, never())
                    .delete(REFRESH_TOKEN_ID);
        }
    }

    @Nested
    @DisplayName("Access Token 블랙리스트 등록")
    class AccessTokenBlacklist {

        @Test
        @DisplayName("Access Token의 남은 유효시간을 기준으로 블랙리스트에 저장한다")
        void savesBlacklistWithRemainingExpiration() {
            // given
            UUID userId = UUID.randomUUID();
            Duration remainingExpiration =
                    Duration.ofSeconds(600);

            prepareValidTokenClaims(userId, userId);

            when(refreshTokenRepository.matches(
                    REFRESH_TOKEN_ID,
                    userId,
                    REFRESH_TOKEN
            )).thenReturn(true);

            when(accessTokenBlacklistRepository.exists(
                    ACCESS_TOKEN_ID
            )).thenReturn(false);

            when(jwtProvider.getRemainingExpiration(accessClaims))
                    .thenReturn(remainingExpiration);

            // when
            logoutService.logout(
                    ACCESS_TOKEN,
                    REFRESH_TOKEN
            );

            // then
            verify(accessTokenBlacklistRepository)
                    .save(
                            ACCESS_TOKEN_ID,
                            remainingExpiration
                    );

            verify(refreshTokenRepository)
                    .delete(REFRESH_TOKEN_ID);
        }
    }

    private void prepareValidTokenClaims(
            UUID accessTokenUserId,
            UUID refreshTokenUserId
    ) {
        when(jwtProvider.validateAccessToken(ACCESS_TOKEN))
                .thenReturn(accessClaims);

        when(jwtProvider.validateRefreshToken(REFRESH_TOKEN))
                .thenReturn(refreshClaims);

        when(jwtProvider.getUserId(accessClaims))
                .thenReturn(accessTokenUserId);

        when(jwtProvider.getUserId(refreshClaims))
                .thenReturn(refreshTokenUserId);

        when(jwtProvider.getJti(accessClaims))
                .thenReturn(ACCESS_TOKEN_ID);

        when(jwtProvider.getJti(refreshClaims))
                .thenReturn(REFRESH_TOKEN_ID);
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