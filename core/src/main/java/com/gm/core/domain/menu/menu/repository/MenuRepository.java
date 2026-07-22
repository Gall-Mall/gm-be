package com.gm.core.domain.menu.menu.repository;

import com.gm.core.domain.menu.menu.model.Menu;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MenuRepository {
    List<Menu> findAll();

    List<Menu> findMenusByCategoryId(UUID categoryId);


}
