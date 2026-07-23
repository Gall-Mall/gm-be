package com.gm.core.domain.menu.repository;

import com.gm.core.domain.menu.model.Allergen;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllergenRepository {
    List<Allergen> findAll();
}
