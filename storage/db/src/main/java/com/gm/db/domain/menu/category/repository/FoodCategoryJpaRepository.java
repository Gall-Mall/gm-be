package com.gm.db.domain.menu.category.repository;

import com.gm.db.domain.menu.category.entity.FoodCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FoodCategoryJpaRepository extends JpaRepository<FoodCategoryEntity, UUID> {
}
