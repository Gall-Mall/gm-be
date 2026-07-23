package com.gm.core.domain.user.model;

import java.util.List;
import java.util.UUID;

public record UserSetting(
        List<UUID> allergenIds,
        List<UUID> preferredMenuIds,
        List<UUID> excludedMenuIds,
        List<UUID> preferredCategoryIds,
        List<UUID> excludedCategoryIds,
        String allergenText,
        String preferredText,
        String excludedText
) {
}
