package com.gm.core.domain.user_setting.user_preference.user_category.repository;

import com.gm.core.domain.user_setting.user_preference.user_category.model.UserCategory;
import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserPreference;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserCategoryRepository {
    List<UserCategory> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    void saveUserCategoryPreference(UUID userId, List<UUID> categoryIds, UserPreference preference);
}
