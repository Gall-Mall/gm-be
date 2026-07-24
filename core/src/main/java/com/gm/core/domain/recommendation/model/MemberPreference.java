package com.gm.core.domain.recommendation.model;

import java.util.Set;
import java.util.UUID;

public record MemberPreference(
        UUID userId,
        Set<UUID> likedMenus,
        Set<UUID> likedCategories,
        Set<UUID> excludeMenus,
        Set<UUID> dislikedCategories,
        Set<UUID> standardAllergens
) {
}
