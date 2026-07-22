package com.gm.core.domain.menu.allergen.model;

import java.util.UUID;

public record Allergen (
        UUID id,
        String name,
        String description
){
}
