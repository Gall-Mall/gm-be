package com.gm.db.domain.menu.allergen.repository;

import com.gm.db.domain.menu.allergen.entity.AllergenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AllergenJpaRepository extends JpaRepository<AllergenEntity, UUID> {
}
