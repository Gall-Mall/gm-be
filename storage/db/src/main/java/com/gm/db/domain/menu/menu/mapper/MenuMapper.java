package com.gm.db.domain.menu.menu.mapper;

import com.gm.core.domain.menu.menu.model.Menu;
import com.gm.db.domain.menu.menu.entity.MenuEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MenuMapper {
    Menu toDomain(MenuEntity entity);
}
