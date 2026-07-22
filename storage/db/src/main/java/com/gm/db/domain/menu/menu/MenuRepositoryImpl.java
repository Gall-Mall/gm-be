package com.gm.db.domain.menu.menu;

import com.gm.core.domain.menu.menu.repository.MenuRepository;
import com.gm.core.domain.menu.menu.model.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

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
}
