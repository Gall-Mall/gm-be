package com.gm.api.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import com.gm.core.domain.auth.exception.AuthErrorCode;
import com.gm.core.domain.auth.exception.AuthException;
import com.gm.core.domain.auth.model.LoginExchange;
import com.gm.core.domain.auth.repository.LoginExchangeRepository;
import com.gm.core.domain.auth.support.LoginExchangeCodeGenerator;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginExchangeService 단위 테스트")
class LoginExchangeServiceTest {

    private static final String EXCHANGE_CODE = "exchange-code";
    private static final String REFRESH_TOKEN_ID = "refresh-token-id";
    private static final String OTHER_REFRESH_TOKEN_ID =
            "other-refresh-token-id";

    private static final long LOGIN_EXCHANGE_EXPIRATION_SECONDS = 180L;

    @Mock
    private LoginExchangeRepository loginExchangeRepository;

    @Mock
    private LoginExchangeCodeGenerator loginExchangeCodeGenerator;

    private LoginExchangeService loginExchangeService;

    @BeforeEach
    void setUp() {
        loginExchangeService = new LoginExchangeService(
                loginExchangeRepository,
                loginExchangeCodeGenerator
        );

        ReflectionTestUtils.setField(
                loginExchangeService,
                "loginExchangeExpirationSeconds",
                LOGIN_EXCHANGE_EXPIRATION_SECONDS
        );
    }

    @Nested
    @DisplayName("로그인 교환 코드 생성")
    class CreateExchangeCode {

        @Test
        @DisplayName("교환 코드를 생성하고 로그인 교환 정보를 저장한다")
        void createsAndSavesExchangeCode() {
            // given
            UUID userId = UUID.randomUUID();
            Duration expectedExpiration =
                    Duration.ofSeconds(LOGIN_EXCHANGE_EXPIRATION_SECONDS);

            when(loginExchangeCodeGenerator.generate())
                    .thenReturn(EXCHANGE_CODE);

            // when
            String result = loginExchangeService.createExchangeCode(
                    userId,
                    REFRESH_TOKEN_ID
            );

            // then
            assertThat(result).isEqualTo(EXCHANGE_CODE);

            verify(loginExchangeCodeGenerator).generate();

            verify(loginExchangeRepository).save(
                    EXCHANGE_CODE,
                    new LoginExchange(userId, REFRESH_TOKEN_ID),
                    expectedExpiration
            );
        }

        @Test
        @DisplayName("회원 ID가 null이면 교환 코드를 생성할 수 없다")
        void throwsExceptionWhenUserIdIsNull() {
            // when & then
            assertThatThrownBy(() ->
                    loginExchangeService.createExchangeCode(
                            null,
                            REFRESH_TOKEN_ID
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("회원 ID는 null일 수 없습니다.");

            verifyNoInteractions(
                    loginExchangeRepository,
                    loginExchangeCodeGenerator
            );
        }

        @Test
        @DisplayName("Refresh Token ID가 null이면 교환 코드를 생성할 수 없다")
        void throwsExceptionWhenRefreshTokenIdIsNull() {
            // given
            UUID userId = UUID.randomUUID();

            // when & then
            assertThatThrownBy(() ->
                    loginExchangeService.createExchangeCode(
                            userId,
                            null
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token ID는 null이거나 공백일 수 없습니다."
                    );

            verifyNoInteractions(
                    loginExchangeRepository,
                    loginExchangeCodeGenerator
            );
        }

        @Test
        @DisplayName("Refresh Token ID가 공백이면 교환 코드를 생성할 수 없다")
        void throwsExceptionWhenRefreshTokenIdIsBlank() {
            // given
            UUID userId = UUID.randomUUID();

            // when & then
            assertThatThrownBy(() ->
                    loginExchangeService.createExchangeCode(
                            userId,
                            "   "
                    )
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(
                            "Refresh Token ID는 null이거나 공백일 수 없습니다."
                    );

            verifyNoInteractions(
                    loginExchangeRepository,
                    loginExchangeCodeGenerator
            );
        }

        @Test
        @DisplayName("만료 시간이 0이면 교환 코드를 생성할 수 없다")
        void throwsExceptionWhenExpirationIsZero() {
            // given
            UUID userId = UUID.randomUUID();

            ReflectionTestUtils.setField(
                    loginExchangeService,
                    "loginExchangeExpirationSeconds",
                    0L
            );

            // when & then
            assertThatThrownBy(() ->
                    loginExchangeService.createExchangeCode(
                            userId,
                            REFRESH_TOKEN_ID
                    )
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(
                            "로그인 교환 코드 만료 시간은 0보다 커야 합니다."
                    );

            verifyNoInteractions(
                    loginExchangeRepository,
                    loginExchangeCodeGenerator
            );
        }

        @Test
        @DisplayName("만료 시간이 음수이면 교환 코드를 생성할 수 없다")
        void throwsExceptionWhenExpirationIsNegative() {
            // given
            UUID userId = UUID.randomUUID();

            ReflectionTestUtils.setField(
                    loginExchangeService,
                    "loginExchangeExpirationSeconds",
                    -1L
            );

            // when & then
            assertThatThrownBy(() ->
                    loginExchangeService.createExchangeCode(
                            userId,
                            REFRESH_TOKEN_ID
                    )
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(
                            "로그인 교환 코드 만료 시간은 0보다 커야 합니다."
                    );

            verifyNoInteractions(
                    loginExchangeRepository,
                    loginExchangeCodeGenerator
            );
        }
    }

    @Nested
    @DisplayName("로그인 교환 코드 소비")
    class ConsumeExchangeCode {

        @Test
        @DisplayName("유효한 교환 코드이면 로그인 교환 정보를 반환한다")
        void consumesValidExchangeCode() {
            // given
            UUID userId = UUID.randomUUID();
            LoginExchange loginExchange =
                    new LoginExchange(userId, REFRESH_TOKEN_ID);

            when(loginExchangeRepository.consume(EXCHANGE_CODE))
                    .thenReturn(Optional.of(loginExchange));

            // when
            LoginExchange result =
                    loginExchangeService.consume(EXCHANGE_CODE);

            // then
            assertThat(result).isEqualTo(loginExchange);
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.refreshTokenId())
                    .isEqualTo(REFRESH_TOKEN_ID);

            verify(loginExchangeRepository)
                    .consume(EXCHANGE_CODE);
        }

        @Test
        @DisplayName("교환 코드가 null이면 존재하지 않음 예외가 발생한다")
        void throwsExceptionWhenExchangeCodeIsNull() {
            assertAuthException(
                    () -> loginExchangeService.consume(null),
                    AuthErrorCode.LOGIN_EXCHANGE_CODE_NOT_FOUND
            );

            verifyNoInteractions(loginExchangeRepository);
        }

        @Test
        @DisplayName("교환 코드가 공백이면 존재하지 않음 예외가 발생한다")
        void throwsExceptionWhenExchangeCodeIsBlank() {
            assertAuthException(
                    () -> loginExchangeService.consume("   "),
                    AuthErrorCode.LOGIN_EXCHANGE_CODE_NOT_FOUND
            );

            verifyNoInteractions(loginExchangeRepository);
        }

        @Test
        @DisplayName("저장된 교환 코드가 없으면 유효하지 않은 코드 예외가 발생한다")
        void throwsExceptionWhenExchangeCodeDoesNotExist() {
            // given
            when(loginExchangeRepository.consume(EXCHANGE_CODE))
                    .thenReturn(Optional.empty());

            // when & then
            assertAuthException(
                    () -> loginExchangeService.consume(EXCHANGE_CODE),
                    AuthErrorCode.INVALID_LOGIN_EXCHANGE_CODE
            );

            verify(loginExchangeRepository)
                    .consume(EXCHANGE_CODE);
        }
    }

    @Nested
    @DisplayName("Refresh Token 세션 검증")
    class ValidateRefreshTokenSession {

        @Test
        @DisplayName("교환 정보의 Refresh Token ID와 현재 세션 ID가 같으면 검증에 성공한다")
        void succeedsWhenRefreshTokenSessionMatches() {
            // given
            UUID userId = UUID.randomUUID();
            LoginExchange loginExchange =
                    new LoginExchange(userId, REFRESH_TOKEN_ID);

            // when
            loginExchangeService.validateRefreshTokenSession(
                    loginExchange,
                    REFRESH_TOKEN_ID
            );

            // then
            assertThat(loginExchange.userId()).isEqualTo(userId);
            assertThat(loginExchange.refreshTokenId())
                    .isEqualTo(REFRESH_TOKEN_ID);
        }

        @Test
        @DisplayName("로그인 교환 정보가 null이면 유효하지 않은 교환 코드 예외가 발생한다")
        void throwsExceptionWhenLoginExchangeIsNull() {
            assertAuthException(
                    () -> loginExchangeService
                            .validateRefreshTokenSession(
                                    null,
                                    REFRESH_TOKEN_ID
                            ),
                    AuthErrorCode.INVALID_LOGIN_EXCHANGE_CODE
            );
        }

        @Test
        @DisplayName("Refresh Token ID가 null이면 토큰 없음 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenIdIsNull() {
            // given
            LoginExchange loginExchange =
                    new LoginExchange(
                            UUID.randomUUID(),
                            REFRESH_TOKEN_ID
                    );

            // when & then
            assertAuthException(
                    () -> loginExchangeService
                            .validateRefreshTokenSession(
                                    loginExchange,
                                    null
                            ),
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
            );
        }

        @Test
        @DisplayName("Refresh Token ID가 공백이면 토큰 없음 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenIdIsBlank() {
            // given
            LoginExchange loginExchange =
                    new LoginExchange(
                            UUID.randomUUID(),
                            REFRESH_TOKEN_ID
                    );

            // when & then
            assertAuthException(
                    () -> loginExchangeService
                            .validateRefreshTokenSession(
                                    loginExchange,
                                    "   "
                            ),
                    AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
            );
        }

        @Test
        @DisplayName("Refresh Token 세션 ID가 일치하지 않으면 세션 불일치 예외가 발생한다")
        void throwsExceptionWhenRefreshTokenSessionDoesNotMatch() {
            // given
            LoginExchange loginExchange =
                    new LoginExchange(
                            UUID.randomUUID(),
                            REFRESH_TOKEN_ID
                    );

            // when & then
            assertAuthException(
                    () -> loginExchangeService
                            .validateRefreshTokenSession(
                                    loginExchange,
                                    OTHER_REFRESH_TOKEN_ID
                            ),
                    AuthErrorCode.LOGIN_EXCHANGE_SESSION_MISMATCH
            );
        }
    }

    @Nested
    @DisplayName("로그인 교환 코드 만료 시간 조회")
    class GetLoginExchangeExpiration {

        @Test
        @DisplayName("설정된 초 단위 만료 시간을 Duration으로 반환한다")
        void returnsConfiguredExpiration() {
            // when
            Duration result =
                    loginExchangeService.getLoginExchangeExpiration();

            // then
            assertThat(result)
                    .isEqualTo(
                            Duration.ofSeconds(
                                    LOGIN_EXCHANGE_EXPIRATION_SECONDS
                            )
                    );
        }

        @Test
        @DisplayName("만료 시간이 0이면 예외가 발생한다")
        void throwsExceptionWhenExpirationIsZero() {
            // given
            ReflectionTestUtils.setField(
                    loginExchangeService,
                    "loginExchangeExpirationSeconds",
                    0L
            );

            // when & then
            assertThatThrownBy(() ->
                    loginExchangeService.getLoginExchangeExpiration()
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(
                            "로그인 교환 코드 만료 시간은 0보다 커야 합니다."
                    );
        }

        @Test
        @DisplayName("만료 시간이 음수이면 예외가 발생한다")
        void throwsExceptionWhenExpirationIsNegative() {
            // given
            ReflectionTestUtils.setField(
                    loginExchangeService,
                    "loginExchangeExpirationSeconds",
                    -180L
            );

            // when & then
            assertThatThrownBy(() ->
                    loginExchangeService.getLoginExchangeExpiration()
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(
                            "로그인 교환 코드 만료 시간은 0보다 커야 합니다."
                    );
        }
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