package com.gm.db.domain.user;

import org.mapstruct.Mapper;

import com.gm.core.domain.user.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toDomain(UserEntity entity);
    UserEntity toEntity(User user);
}