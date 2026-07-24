package com.gm.core.domain.recommendation.model;

import java.util.UUID;

public record ScoredMenu(
        UUID menuId,
        double score
) {
}
