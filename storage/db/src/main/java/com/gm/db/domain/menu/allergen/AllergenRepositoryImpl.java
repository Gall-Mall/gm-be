package com.gm.db.domain.menu.allergen;

import com.gm.core.domain.menu.allergen.repository.AllergenRepository;
import com.gm.core.domain.menu.allergen.model.Allergen;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class AllergenRepositoryImpl implements AllergenRepository {

    private final AllergenJpaRepository allergenJpaRepository;
    private final AllergenMapper allergenMapper;

    @Override
    public List<Allergen> findAll() {
        return allergenJpaRepository
                .findAll()
                .stream()
                .map(allergenMapper::toDomain)
                .toList();
    }
}
