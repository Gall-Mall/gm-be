package com.gm.core.domain.menu.repository;

import com.gm.core.domain.menu.model.Allergen;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AllergenRepository {
    List<Allergen> findAll();

    Set<UUID> findExistingIds(Set<UUID> ids);
}
