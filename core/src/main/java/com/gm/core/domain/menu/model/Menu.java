package com.gm.core.domain.menu.model;

import java.util.UUID;

public record Menu (
        UUID id,
        UUID categoryId,
        String name,
        String imageUrl
){
}
