package com.gm.core.domain.onboarding.model;

import com.gm.core.domain.user_setting.model.UserSetting;

public record Onboarding(
        boolean termsAgreed,
        UserSetting userSetting
) {
}
