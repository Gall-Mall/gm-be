package com.gm.db.domain.user.repository;

import com.gm.core.domain.user.model.UserMenu;
import com.gm.db.domain.user.entity.UserMenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserMenuJpaRepository extends JpaRepository<UserMenuEntity, UUID> {
    List<UserMenu> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
