package com.gm.core.domain.user.model;

import java.util.UUID;

public record UserAllergen(
         UUID id,
         UUID userId,
         UUID allergenId
) {
}
