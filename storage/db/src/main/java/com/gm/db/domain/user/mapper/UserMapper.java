package com.gm.db.domain.user.mapper;

import com.gm.db.domain.user.entity.UserEntity;
import org.mapstruct.Mapper;

import com.gm.core.domain.user.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toDomain(UserEntity entity);
    UserEntity toEntity(User user);
}