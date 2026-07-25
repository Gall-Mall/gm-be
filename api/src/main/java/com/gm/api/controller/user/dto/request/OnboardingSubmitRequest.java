package com.gm.api.controller.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.gm.core.domain.user.model.Onboarding;

/**
 * 약관 동의와 최초 회원 설정을 함께 제출한다.
 */
public record OnboardingSubmitRequest(
        @NotNull
        Boolean termsAgreed,

        @Valid
        @NotNull
        UserSettingRequest userSetting
) {

    public Onboarding toDomain() {
        return new Onboarding(
                termsAgreed,
                userSetting.toDomain()
        );
    }
}
