package com.gm.db.domain.menu.menu;

import com.gm.core.domain.menu.menu.model.Menu;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MenuMapper {
    Menu toDomain(MenuEntity entity);
}
