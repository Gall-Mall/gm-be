package com.gm.db.domain.user.repository;

import java.util.Optional;
import java.util.UUID;

import com.gm.core.domain.user.model.Provider;
import com.gm.db.domain.user.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByProviderAndProviderId(Provider provider, String providerId);
}