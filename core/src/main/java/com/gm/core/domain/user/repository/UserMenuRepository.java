package com.gm.core.domain.user.repository;

import com.gm.core.domain.user.model.UserMenu;
import com.gm.core.domain.user.model.UserMenuPreference;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserMenuRepository {
    List<UserMenu> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    void addUserMenuPreference(UUID userId, List<UUID> menuIds, UserMenuPreference preference);
}
