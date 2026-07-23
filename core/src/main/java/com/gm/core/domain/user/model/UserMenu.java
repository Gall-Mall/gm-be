package com.gm.core.domain.user.model;

import java.util.UUID;

public record UserMenu(
        UUID userId,
        UUID menuId,
        UserMenuPreference preference
) {
}
