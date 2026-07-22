package com.gm.db.domain.menu.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MenuJpaRepository extends JpaRepository<MenuEntity, UUID> {
    List<MenuEntity> findByCategoryId(UUID categoryId);
}
