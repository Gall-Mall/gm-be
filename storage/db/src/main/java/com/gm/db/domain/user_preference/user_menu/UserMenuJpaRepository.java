package com.gm.db.domain.user_preference.user_menu;

import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserMenuJpaRepository extends JpaRepository<UserMenuEntity, UUID> {
    List<UserMenu> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
