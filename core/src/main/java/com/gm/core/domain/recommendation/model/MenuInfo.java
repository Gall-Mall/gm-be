package com.gm.core.domain.recommendation.model;

import java.util.Set;
import java.util.UUID;

public record MenuInfo(
        UUID menuId,
        UUID categoryId,
        Set<UUID> allergenIds
) {
}
