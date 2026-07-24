package com.gm.core.domain.user.repository;

import java.util.Optional;
import java.util.UUID;

import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserResult;

public interface UserRepository {

    Optional<User> findById(UUID id);
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
    Optional<UserResult> findResultByProviderAndProviderId(Provider provider, String providerId);

    User save(User user);
    UserResult saveResult(User user);

    void updateUserStatus(UUID userId);

    void saveUserInputText(UUID userId, String allergenText, String preferredText, String excludedText);
}