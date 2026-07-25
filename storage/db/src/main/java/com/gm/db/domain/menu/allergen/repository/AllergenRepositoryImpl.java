package com.gm.db.domain.menu.allergen.repository;

import com.gm.core.domain.menu.repository.AllergenRepository;
import com.gm.core.domain.menu.model.Allergen;
import com.gm.db.common.entity.BaseEntity;
import com.gm.db.domain.menu.allergen.mapper.AllergenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Override
    public Set<UUID> findExistingIds(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }

        return allergenJpaRepository.findAllById(ids).stream()
                .map(BaseEntity::getId)
                .collect(Collectors.toSet());
    }
}
