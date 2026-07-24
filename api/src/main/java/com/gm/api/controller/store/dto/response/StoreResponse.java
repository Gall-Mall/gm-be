package com.gm.api.controller.store.dto.response;

import com.gm.core.domain.store.model.Provider;
import com.gm.core.domain.store.model.Store;

/**
 * 외부 장소 검색으로 찾은 음식점 정보.
 */
public record StoreResponse(
        String externalPlaceId,
        String name,
        String address,
        String categoryName,
        String url,
        double longitude,
        double latitude,
        Provider provider,
        int distanceM
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.externalPlaceId(),
                store.name(),
                store.address(),
                store.categoryName(),
                store.url(),
                store.coordinate().x(),
                store.coordinate().y(),
                store.provider(),
                Integer.parseInt(store.distance())
        );
    }

}
