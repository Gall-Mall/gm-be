package com.gm.core.domain.store.model;

public record Store (
        String placeId,
        String placeName,
        String roadAddress,
        String categoryName,
        String placeUrl,
        Coordinate coordinate,
        Provider provider,
        String distance
        ) {
}
