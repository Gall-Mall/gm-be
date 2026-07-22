package com.gm.db.domain.user_preference.user_category;

import com.gm.core.domain.user_setting.user_preference.user_category.model.UserCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserCategoryJpaRepository extends JpaRepository<UserCategoryEntity, UUID> {
    List<UserCategoryEntity> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
