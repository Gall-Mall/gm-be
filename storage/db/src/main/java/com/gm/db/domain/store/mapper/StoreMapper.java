package com.gm.db.domain.store.mapper;

import com.gm.core.domain.store.model.Store;
import com.gm.db.domain.store.entity.StoreEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    @Mapping(source = "voteSessionId", target = "voteSessionId")
    @Mapping(target = "selected", constant = "false")
    @Mapping(source = "store.coordinate.y", target = "latitude")
    @Mapping(source = "store.coordinate.x", target = "longitude")
    StoreEntity toStoreEntity(UUID voteSessionId, Store store);
}

