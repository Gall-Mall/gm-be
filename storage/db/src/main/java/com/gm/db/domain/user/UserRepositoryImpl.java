package com.gm.db.domain.user;

import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.repository.UserRepository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(UserEntity::toDomainModel);
    }

    @Override
    public Optional<User> findByProviderAndProviderId(String provider, String providerId) {
        return userJpaRepository
                .findByProviderAndProviderId(provider, providerId)
                .map(UserEntity::toDomainModel);
    }

    @Override
    public User save(User user) {
        UserEntity userEntity = new UserEntity(
                user.name(),
                user.nickname(),
                user.status(),
                user.provider(),
                user.providerId(),
                user.phone(),
                user.email(),
                user.termsAgreed()
        );

        UserEntity savedUserEntity =
                userJpaRepository.save(userEntity);

        return savedUserEntity.toDomainModel();
    }
}