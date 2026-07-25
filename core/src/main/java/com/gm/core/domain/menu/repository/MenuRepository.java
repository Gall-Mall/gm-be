package com.gm.core.domain.menu.repository;

import com.gm.core.domain.menu.model.Menu;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface MenuRepository {
    List<Menu> findAll();

    List<Menu> findMenusByCategoryId(UUID categoryId);

    Set<UUID> findExistingIds(Set<UUID> ids);
}
