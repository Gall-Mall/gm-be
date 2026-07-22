package com.gm.core.domain.user_setting.user_preference.user_category.model;

import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserPreference;
import java.util.UUID;

public record UserCategory(
        UUID id,
        UUID userId,
        UUID categoryId,
        UserPreference preference
) {
}
