package com.gm.core.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.gm.core.domain.user.exception.UserException;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserResult;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    private UserService userService;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
        userId = UUID.randomUUID();
        user = new User(
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

    @Nested
    @DisplayName("회원 ID 조회")
    class FindById {

        @Test
        @DisplayName("회원이 존재하면 회원을 반환한다")
        void findById_success() {
            // given
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // when
            User result = userService.findById(userId);

            // then
            assertThat(result).isEqualTo(user);
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 UserException이 발생한다")
        void findById_notFound() {
            // given
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.findById(userId)).isInstanceOf(UserException.class);
            verify(userRepository).findById(userId);
        }
    }

    @Nested
    @DisplayName("소셜 로그인 회원 조회 또는 생성")
    class FindOrCreate {

        @Test
        @DisplayName("기존 회원이 있으면 새로 저장하지 않고 기존 회원을 반환한다")
        void findOrCreate_existingUser() {
            // given
            when(userRepository.findByProviderAndProviderId(Provider.NAVER, "naver-provider-id")).thenReturn(Optional.of(user));

            // when
            User result = userService.findOrCreate(
                    "홍길동",
                    Provider.NAVER,
                    "naver-provider-id",
                    "01012345678",
                    "user@example.com"
            );

            // then
            assertThat(result).isEqualTo(user);

            verify(userRepository)
                    .findByProviderAndProviderId(Provider.NAVER, "naver-provider-id");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("기존 회원이 없으면 신규 회원을 생성하고 저장한다")
        void findOrCreate_newUser() {
            // given
            when(userRepository.findByProviderAndProviderId(Provider.NAVER, "naver-provider-id")).thenReturn(Optional.empty());

            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            User result = userService.findOrCreate(
                    "홍길동",
                    Provider.NAVER,
                    "naver-provider-id",
                    "01012345678",
                    "user@example.com"
            );

            // then
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(result.nickname()).isEqualTo("홍길동");
            assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(result.provider()).isEqualTo(Provider.NAVER);
            assertThat(result.providerId()).isEqualTo("naver-provider-id");
            assertThat(result.phone()).isEqualTo("01012345678");
            assertThat(result.email()).isEqualTo("user@example.com");
            assertThat(result.termsAgreed()).isFalse();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();

            assertThat(savedUser.provider()).isEqualTo(Provider.NAVER);
            assertThat(savedUser.providerId()).isEqualTo("naver-provider-id");
        }
    }

    @Nested
    @DisplayName("OAuth2 로그인용 회원 조회 또는 생성")
    class FindOrCreateWithId {

        @Test
        @DisplayName("기존 회원이 있으면 UUID와 회원 정보를 반환한다")
        void findOrCreateWithId_existingUser() {
            // given
            UserResult expected = UserResult.of(userId, user);

            when(userRepository.findResultByProviderAndProviderId(Provider.NAVER, "naver-provider-id")).thenReturn(Optional.of(expected));

            // when
            UserResult result = userService.findOrCreateWithId(
                    "홍길동",
                    Provider.NAVER,
                    "naver-provider-id",
                    "01012345678",
                    "user@example.com"
            );

            // then
            assertThat(result).isEqualTo(expected);
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.user()).isEqualTo(user);

            verify(userRepository, never()).saveResult(any(User.class));
        }

        @Test
        @DisplayName("기존 회원이 없으면 신규 회원을 저장하고 UUID와 회원 정보를 반환한다")
        void findOrCreateWithId_newUser() {
            // given
            when(userRepository.findResultByProviderAndProviderId(Provider.NAVER, "naver-provider-id")).thenReturn(Optional.empty());

            when(userRepository.saveResult(any(User.class)))
                    .thenAnswer(invocation -> {
                        User createdUser = invocation.getArgument(0);
                        return UserResult.of(userId, createdUser);
                    });

            // when
            UserResult result = userService.findOrCreateWithId(
                    "홍길동",
                    Provider.NAVER,
                    "naver-provider-id",
                    "01012345678",
                    "user@example.com"
            );

            // then
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.user().status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(result.user().provider()).isEqualTo(Provider.NAVER);
            assertThat(result.user().providerId()).isEqualTo("naver-provider-id");

            verify(userRepository).saveResult(any(User.class));
        }
    }
}