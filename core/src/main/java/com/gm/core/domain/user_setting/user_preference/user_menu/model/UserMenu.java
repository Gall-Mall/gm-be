package com.gm.core.domain.user_setting.user_preference.user_menu.model;

import java.util.UUID;

public record UserMenu(
        UUID userId,
        UUID menuId,
        UserPreference preference
) {
}
