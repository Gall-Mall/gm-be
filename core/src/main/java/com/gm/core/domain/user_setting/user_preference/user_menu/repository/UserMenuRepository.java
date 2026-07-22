package com.gm.core.domain.user_setting.user_preference.user_menu.repository;

import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserMenu;
import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserPreference;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserMenuRepository {
    List<UserMenu> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    void addUserMenuPreference(UUID userId, List<UUID> menuIds, UserPreference preference);
}
