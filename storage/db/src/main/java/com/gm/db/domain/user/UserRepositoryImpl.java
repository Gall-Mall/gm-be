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
    private final UserMapper userMapper;

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByProviderAndProviderId(String provider, String providerId) {
        return userJpaRepository
                .findByProviderAndProviderId(provider, providerId)
                .map(userMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity userEntity = userMapper.toEntity(user);
        UserEntity savedUserEntity = userJpaRepository.save(userEntity);

        return userMapper.toDomain(savedUserEntity);
    }
}