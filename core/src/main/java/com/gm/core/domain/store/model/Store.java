package com.gm.core.domain.store.model;

public record Store (
        String externalPlaceId,
        String name,
        String address,
        String categoryName,
        String url,
        Coordinate coordinate,
        Provider provider,
        String distance
        ) {
}
