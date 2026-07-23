package com.gm.db.domain.user.mapper;

import com.gm.core.domain.user.model.UserCategory;
import com.gm.db.domain.user.entity.UserCategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserCategoryMapper {
    UserCategory toDomain(UserCategoryEntity entity);
    UserCategoryEntity toEntity(UserCategory domain);
}
