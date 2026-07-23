package com.gm.core.domain.user.model;

public record Onboarding(
        boolean termsAgreed,
        UserSetting userSetting
) {
}
