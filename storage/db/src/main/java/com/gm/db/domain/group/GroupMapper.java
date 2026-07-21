package com.gm.db.domain.group;

import org.mapstruct.Mapper;

import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.NewGroup;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    GroupEntity toEntity(NewGroup newGroup);

    Group toDomainModel(GroupEntity entity, int memberCount);
}
