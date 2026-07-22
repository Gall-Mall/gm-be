package com.gm.core.domain.user_setting.user_preference.user_allergen.model;

import java.util.UUID;

public record UserAllergen(
         UUID id,
         UUID userId,
         UUID allergenId
) {
}
