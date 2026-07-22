package com.gm.db.domain.menu.allergen;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AllergenJpaRepository extends JpaRepository<AllergenEntity, UUID> {
}
