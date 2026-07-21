package com.gm.core.domain.user.repository;

import java.util.Optional;
import java.util.UUID;

import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;

public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    User save(User user);
}