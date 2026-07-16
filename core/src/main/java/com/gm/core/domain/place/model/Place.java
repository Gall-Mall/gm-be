package com.gm.core.domain.place.model;

public record Place (
        String name,
        String url,
        String address,
        double latitude,
        double longitude,
        int distance
) {
}
