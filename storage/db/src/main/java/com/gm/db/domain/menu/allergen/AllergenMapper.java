package com.gm.db.domain.menu.allergen;

import com.gm.core.domain.menu.allergen.model.Allergen;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AllergenMapper {

    Allergen toDomain(AllergenEntity entity);
}
