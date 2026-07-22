package com.gm.db.domain.user_preference.user_allergen.repository;

import com.gm.core.domain.user_setting.user_preference.user_allergen.model.UserAllergen;
import com.gm.db.domain.user_preference.user_allergen.entity.UserAllergenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAllergenJpaRepository extends JpaRepository<UserAllergenEntity, UUID> {
    List<UserAllergen> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
