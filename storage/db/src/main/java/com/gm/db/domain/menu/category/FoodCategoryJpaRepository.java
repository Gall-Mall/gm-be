package com.gm.db.domain.menu.category;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FoodCategoryJpaRepository extends JpaRepository<FoodCategoryEntity, UUID> {
}
