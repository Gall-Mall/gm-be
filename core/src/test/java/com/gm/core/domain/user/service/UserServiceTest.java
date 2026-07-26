package com.gm.core.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gm.core.domain.user.exception.UserErrorCode;
import com.gm.core.domain.user.exception.UserException;
import com.gm.core.domain.user.model.Onboarding;
import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserResult;
import com.gm.core.domain.user.model.UserSetting;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.core.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAllergenService userAllergenService;

    @Mock
    private UserCategoryService userCategoryService;

    @Mock
    private UserMenuService userMenuService;


    @Test
    @DisplayName("회원 UUID로 기존 회원을 조회한다")
    void findByIdReturnsExistingUser() {
        // given
        UUID userId = UUID.randomUUID();
        User user = createUser(UserStatus.ACTIVE);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserService userService = new UserService(userRepository, userAllergenService, userCategoryService, userMenuService);

        // when
        User result = userService.findById(userId);

        // then
        assertThat(result).isEqualTo(user);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("회원 UUID에 해당하는 회원이 없으면 USER_NOT_FOUND 예외가 발생한다")
    void findByIdThrowsWhenUserDoesNotExist() {
        // given
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        UserService userService = new UserService(userRepository, userAllergenService, userCategoryService, userMenuService);

        // when & then
        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(UserException.class)
                .satisfies(exception -> {
                    UserException userException = (UserException) exception;

                    assertThat(userException.getErrorCode())
                            .isEqualTo(UserErrorCode.USER_NOT_FOUND);
                });

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("기존 네이버 회원이면 새로 저장하지 않고 기존 회원 정보를 반환한다")
    void findOrCreateWithIdReturnsExistingUser() {
        // given
        UUID userId = UUID.randomUUID();
        User existingUser = createUser(UserStatus.ACTIVE);
        UserResult existingResult = UserResult.of(userId, existingUser);

        when(userRepository.findResultByProviderAndProviderId(
                Provider.NAVER,
                "naver-provider-id"
        )).thenReturn(Optional.of(existingResult));

        UserService userService = new UserService(userRepository, userAllergenService, userCategoryService, userMenuService);

        // when
        UserResult result = userService.findOrCreateWithId(
                "홍길동",
                Provider.NAVER,
                "naver-provider-id",
                "010-1234-5678",
                "user@example.com"
        );

        // then
        assertThat(result).isEqualTo(existingResult);

        verify(userRepository, never())
                .saveResult(any(User.class));
    }

    @Test
    @DisplayName("기존 네이버 회원이 없으면 ONBOARDING 상태로 신규 회원을 생성한다")
    void findOrCreateWithIdCreatesOnboardingUser() {
        // given
        UUID generatedUserId = UUID.randomUUID();

        when(userRepository.findResultByProviderAndProviderId(
                Provider.NAVER,
                "new-provider-id"
        )).thenReturn(Optional.empty());

        when(userRepository.saveResult(any(User.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);

                    return UserResult.of(generatedUserId, savedUser);
                });

        UserService userService = new UserService(userRepository, userAllergenService, userCategoryService, userMenuService);

        // when
        UserResult result = userService.findOrCreateWithId(
                "신규 사용자",
                Provider.NAVER,
                "new-provider-id",
                "010-9999-9999",
                "new@example.com"
        );

        // then
        assertThat(result.userId()).isEqualTo(generatedUserId);
        assertThat(result.user().name()).isEqualTo("신규 사용자");
        assertThat(result.user().nickname()).isEqualTo("신규 사용자");
        assertThat(result.user().status()).isEqualTo(UserStatus.ONBOARDING);
        assertThat(result.user().provider()).isEqualTo(Provider.NAVER);
        assertThat(result.user().providerId()).isEqualTo("new-provider-id");
        assertThat(result.user().phone()).isEqualTo("010-9999-9999");
        assertThat(result.user().email()).isEqualTo("new@example.com");
        assertThat(result.user().termsAgreed()).isFalse();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).saveResult(captor.capture());

        User savedUser = captor.getValue();

        assertThat(savedUser.status()).isEqualTo(UserStatus.ONBOARDING);
        assertThat(savedUser.termsAgreed()).isFalse();
    }

    @Test
    @DisplayName("약관에 동의한 온보딩이면 사용자 설정을 저장하고 회원을 활성화한다")
    void submitOnboardingSavesSettingsAndCompletesOnboarding() {
        // given
        UUID userId = UUID.randomUUID();
        UUID allergenId = UUID.randomUUID();
        UUID preferredMenuId = UUID.randomUUID();
        UUID excludedMenuId = UUID.randomUUID();
        UUID preferredCategoryId = UUID.randomUUID();
        UUID excludedCategoryId = UUID.randomUUID();

        UserSetting userSetting = new UserSetting(
                List.of(allergenId),
                List.of(preferredMenuId),
                List.of(excludedMenuId),
                List.of(preferredCategoryId),
                List.of(excludedCategoryId),
                "새우",
                "매운 음식",
                "고수"
        );
        Onboarding onboarding = new Onboarding(true, userSetting);
        UserService userService = new UserService(
                userRepository,
                userAllergenService,
                userCategoryService,
                userMenuService
        );

        // when
        userService.submitOnboarding(userId, onboarding);

        // then
        verify(userRepository).saveUserInputText(userId, "새우", "매운 음식", "고수");
        verify(userAllergenService).saveUserAllergens(userId, List.of(allergenId));
        verify(userMenuService).saveUserMenuPreference(
                userId,
                List.of(preferredMenuId),
                List.of(excludedMenuId)
        );
        verify(userCategoryService).saveUserCategoryPreference(
                userId,
                List.of(preferredCategoryId),
                List.of(excludedCategoryId)
        );
        verify(userRepository).updateUserStatus(userId);
    }

    @Test
    @DisplayName("필수 약관에 동의하지 않으면 온보딩을 완료하지 않는다")
    void submitOnboardingDoesNotCompleteWithoutTermsAgreement() {
        // given
        UUID userId = UUID.randomUUID();
        Onboarding onboarding = new Onboarding(false, emptyUserSetting());
        UserService userService = new UserService(
                userRepository,
                userAllergenService,
                userCategoryService,
                userMenuService
        );

        // when & then
        assertThatThrownBy(() -> userService.submitOnboarding(userId, onboarding))
                .isInstanceOf(UserException.class)
                .satisfies(exception -> assertThat(((UserException) exception).getErrorCode())
                        .isEqualTo(UserErrorCode.TERMS_NOT_AGREED));

        verify(userRepository, never()).updateUserStatus(userId);
    }

    @Test
    @DisplayName("온보딩에서 같은 메뉴를 선호와 제외에 함께 선택하면 예외가 발생한다")
    void submitOnboardingRejectsOverlappingMenuPreferences() {
        // given
        UUID userId = UUID.randomUUID();
        UUID duplicatedMenuId = UUID.randomUUID();
        UserSetting userSetting = new UserSetting(
                List.of(),
                List.of(duplicatedMenuId),
                List.of(duplicatedMenuId),
                List.of(),
                List.of(),
                "",
                "",
                ""
        );
        UserService userService = new UserService(
                userRepository,
                userAllergenService,
                userCategoryService,
                userMenuService
        );

        // when & then
        assertThatThrownBy(() -> userService.submitOnboarding(userId, new Onboarding(true, userSetting)))
                .isInstanceOf(UserException.class)
                .satisfies(exception -> assertThat(((UserException) exception).getErrorCode())
                        .isEqualTo(UserErrorCode.MENU_PREFERENCE_CONFLICT));

        verify(userRepository, never()).updateUserStatus(userId);
    }

    @Test
    @DisplayName("nullable 자유 입력값도 온보딩 설정으로 저장할 수 있다")
    void submitOnboardingAllowsNullableInputText() {
        // given
        UUID userId = UUID.randomUUID();
        UserService userService = new UserService(
                userRepository,
                userAllergenService,
                userCategoryService,
                userMenuService
        );

        // when
        userService.submitOnboarding(userId, new Onboarding(true, nullableTextUserSetting()));

        // then
        verify(userRepository).saveUserInputText(userId, null, null, null);
        verify(userRepository).updateUserStatus(userId);
    }

    private UserSetting emptyUserSetting() {
        return new UserSetting(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "",
                "",
                ""
        );
    }

    private UserSetting nullableTextUserSetting() {
        return new UserSetting(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null
        );
    }

    private User createUser(UserStatus status) {
        return new User(
                "홍길동",
                "길동",
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
