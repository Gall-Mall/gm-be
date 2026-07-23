package com.gm.db.domain.menu.allergen.mapper;

import com.gm.core.domain.menu.model.Allergen;
import com.gm.db.domain.menu.allergen.entity.AllergenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AllergenMapper {

    Allergen toDomain(AllergenEntity entity);
}
