package com.gm.db.domain.store.mapper;

import com.gm.core.domain.store.model.Store;
import com.gm.db.domain.store.StoreEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
    public interface StoreMapper {

        @Mapping(target = "selected", constant = "false")
        @Mapping(source = "placeName", target = "name")
        @Mapping(source = "placeUrl", target = "url")
        @Mapping(source = "roadAddress", target = "address")
        @Mapping(source = "coordinate.y", target = "latitude")
        @Mapping(source = "coordinate.x", target = "longitude")
        @Mapping(source = "distance", target = "distance")
        @Mapping(source = "placeId", target = "externalPlaceId")
        @Mapping(source = "provider", target = "provider")
        StoreEntity toStoreEntity(Store store);

    }

