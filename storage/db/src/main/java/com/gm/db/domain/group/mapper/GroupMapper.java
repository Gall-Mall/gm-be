package com.gm.db.domain.group.mapper;

import org.mapstruct.Mapper;

import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.db.domain.group.entity.DiningGroupEntity;
import com.gm.db.domain.group.projection.GroupDetailProjection;
import com.gm.db.domain.group.projection.GroupSummaryProjection;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    DiningGroupEntity toEntity(NewGroup newGroup);

    Group toDomainModel(DiningGroupEntity entity, int memberCount);

    Group toDomainModel(GroupSummaryProjection projection);

    Group toDomainModel(GroupDetailProjection projection);
}
