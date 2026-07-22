package com.gm.db.domain.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserResult;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toDomain(UserEntity entity);
    UserEntity toEntity(User user);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "user", expression = "java(toDomain(entity))")
    UserResult toResult(UserEntity entity);
}