package com.gm.db.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.UserStatus;
import com.gm.db.domain.user.entity.UserEntity;
import com.gm.db.domain.user.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private UserMapper userMapper;

    @Test
    @DisplayName("온보딩을 완료하면 약관 동의와 사용자 상태를 관리 엔티티에 반영한다")
    void updateUserStatusCompletesOnboarding() {
        // given
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = createOnboardingUserEntity();
        UserRepositoryImpl repository = new UserRepositoryImpl(userJpaRepository, userMapper);

        when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        // when
        repository.updateUserStatus(userId);

        // then
        assertThat(userEntity.getTermsAgreed()).isTrue();
        assertThat(userEntity.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userJpaRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("사용자 자유 입력값을 기존 관리 엔티티에 반영한다")
    void saveUserInputTextUpdatesManagedEntity() {
        // given
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = createOnboardingUserEntity();
        UserRepositoryImpl repository = new UserRepositoryImpl(userJpaRepository, userMapper);

        when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        // when
        repository.saveUserInputText(userId, "새우", "매운 음식", "고수");

        // then
        assertThat(userEntity.getCustomAllergenText()).isEqualTo("새우");
        assertThat(userEntity.getPreferenceText()).isEqualTo("매운 음식");
        assertThat(userEntity.getExcludeFoodText()).isEqualTo("고수");
        verify(userJpaRepository, never()).save(any(UserEntity.class));
    }

    private UserEntity createOnboardingUserEntity() {
        return new UserEntity(
                "홍길동",
                "길동",
                UserStatus.ONBOARDING,
                Provider.NAVER,
                "naver-provider-id",
                "010-1234-5678",
                "user@example.com",
                false
        );
    }
}
