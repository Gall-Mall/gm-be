package com.gm.core.domain.user.model;

import java.util.UUID;

public record UserCategory(
        UUID id,
        UUID userId,
        UUID categoryId,
        UserCategoryPreference preference
) {
}
