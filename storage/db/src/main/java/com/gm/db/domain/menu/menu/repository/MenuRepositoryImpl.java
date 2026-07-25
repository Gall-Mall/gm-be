package com.gm.db.domain.menu.menu.repository;

import com.gm.core.domain.menu.repository.MenuRepository;
import com.gm.core.domain.menu.model.Menu;
import com.gm.db.common.entity.BaseEntity;
import com.gm.db.domain.menu.menu.mapper.MenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MenuRepositoryImpl implements MenuRepository {
    private final MenuJpaRepository menuJpaRepository;
    private final MenuMapper menuMapper;

    @Override
    public List<Menu> findAll() {
        return menuJpaRepository.findAll().stream().map(menuMapper::toDomain).toList();
    }

    @Override
    public List<Menu> findMenusByCategoryId(UUID categoryId) {
        return menuJpaRepository.findByCategoryId(categoryId).stream().map(menuMapper::toDomain).toList();
    }

    @Override
    public Set<UUID> findExistingIds(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }

        return menuJpaRepository.findAllById(ids).stream()
                .map(BaseEntity::getId)
                .collect(Collectors.toSet());
    }
}
