package com.gm.db.domain.user.preference.user_category.mapper;

import com.gm.core.domain.user.model.UserCategory;
import com.gm.db.domain.user.preference.user_category.entity.UserCategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserCategoryMapper {
    UserCategory toDomain(UserCategoryEntity entity);
    UserCategoryEntity toEntity(UserCategory domain);
}
