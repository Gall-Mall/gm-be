package com.gm.db.domain.user.mapper;

import org.mapstruct.Mapper;

import com.gm.core.domain.user.model.User;
import com.gm.db.domain.user.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toDomain(UserEntity entity);
    UserEntity toEntity(User user);
}