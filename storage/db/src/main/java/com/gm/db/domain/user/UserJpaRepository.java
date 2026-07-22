package com.gm.db.domain.user;

import java.util.Optional;
import java.util.UUID;

import com.gm.core.domain.user.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByProviderAndProviderId(Provider provider, String providerId);
}