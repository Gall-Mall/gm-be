package com.gm.db.domain.user.repository;

import com.gm.core.domain.user.model.UserMenu;
import com.gm.core.domain.user.repository.UserMenuRepository;
import com.gm.core.domain.user.model.UserMenuPreference;
import com.gm.db.domain.user.entity.UserMenuEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
    public void addUserMenuPreference(UUID userId, List<UUID> preferredMenuIds, List<UUID> excludedMenuIds) {

        List<UserMenuEntity> userMenuEntities = Stream.concat(
                preferredMenuIds.stream()
                        .map(menuId -> new UserMenuEntity(
                                userId,
                                menuId,
                                UserMenuPreference.LIKE
                        )),
                excludedMenuIds.stream()
                        .map(menuId -> new UserMenuEntity(
                                userId,
                                menuId,
                                UserMenuPreference.EXCLUDE
                        ))
        ).toList();

        userMenuJpaRepository.saveAll(userMenuEntities);
    }
}
