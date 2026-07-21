package com.gm.db.domain.store.mapper;

import com.gm.core.domain.store.model.Store;
import com.gm.db.domain.store.StoreEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    @Mapping(target = "selected", constant = "false")
    @Mapping(source = "coordinate.y", target = "latitude")
    @Mapping(source = "coordinate.x", target = "longitude")
    StoreEntity toStoreEntity(Store store);

}

