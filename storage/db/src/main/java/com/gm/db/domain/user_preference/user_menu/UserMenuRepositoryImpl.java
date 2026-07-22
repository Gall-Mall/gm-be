package com.gm.db.domain.user_preference.user_menu;

import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserMenu;
import com.gm.core.domain.user_setting.user_preference.user_menu.repository.UserMenuRepository;
import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class UserMenuRepositoryImpl implements UserMenuRepository {

    private final UserMenuJpaRepository userMenuJpaRepository;

    @Override
    public List<UserMenu> findByUserId(UUID userId) {
        return userMenuJpaRepository.findByUserId(userId);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        userMenuJpaRepository.deleteByUserId(userId);
        userMenuJpaRepository.flush();
    }

    @Override
    public void addUserMenuPreference(UUID userId, List<UUID> menuIds, UserPreference preference) {
        for (UUID uuid : menuIds) {
            userMenuJpaRepository.save(new UserMenuEntity(userId, uuid, preference));
        }
    }
}
