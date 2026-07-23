package com.gm.db.domain.user.preference.user_category.repository;

import com.gm.db.domain.user.preference.user_category.entity.UserCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserCategoryJpaRepository extends JpaRepository<UserCategoryEntity, UUID> {
    List<UserCategoryEntity> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
