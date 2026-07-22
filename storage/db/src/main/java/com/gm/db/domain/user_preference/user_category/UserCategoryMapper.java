package com.gm.db.domain.user_preference.user_category;

import com.gm.core.domain.user_setting.user_preference.user_category.model.UserCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserCategoryMapper {
    UserCategory toDomain(UserCategoryEntity entity);
    UserCategoryEntity toEntity(UserCategory domain);
}
