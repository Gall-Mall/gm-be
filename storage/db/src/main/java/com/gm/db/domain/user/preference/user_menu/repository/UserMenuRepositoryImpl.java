package com.gm.db.domain.user.preference.user_menu.repository;

import com.gm.core.domain.user.model.UserMenu;
import com.gm.core.domain.user.repository.UserMenuRepository;
import com.gm.core.domain.user.model.UserMenuPreference;
import com.gm.db.domain.user.preference.user_menu.entity.UserMenuEntity;
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
    public void addUserMenuPreference(UUID userId, List<UUID> menuIds, UserMenuPreference preference) {
        List<UserMenuEntity> list = menuIds
                .stream()
                .map(menuId -> new UserMenuEntity(userId, menuId, preference))
                .toList();
        userMenuJpaRepository.saveAll(list);
    }
}
