package com.gm.db.domain.user.repository;

import com.gm.db.domain.user.entity.UserEntity;
import com.gm.db.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.User;
import com.gm.core.domain.user.model.UserResult;
import com.gm.core.domain.user.repository.UserRepository;
import com.gm.db.domain.user.mapper.UserMapper;
import com.gm.db.domain.user.entity.UserEntity;

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
    public Optional<User> findByProviderAndProviderId(Provider provider, String providerId) {
        return userJpaRepository
                .findByProviderAndProviderId(provider, providerId)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<UserResult> findResultByProviderAndProviderId(Provider provider, String providerId) {
        return userJpaRepository
                .findByProviderAndProviderId(provider, providerId)
                .map(entity -> UserResult.of(entity.getId(), userMapper.toDomain(entity)));
    }

    @Override
    public User save(User user) {
        UserEntity userEntity = userMapper.toEntity(user);
        UserEntity savedUserEntity = userJpaRepository.save(userEntity);

        return userMapper.toDomain(savedUserEntity);
    }

    @Override
    public UserResult saveResult(User user) {
        UserEntity entity = userMapper.toEntity(user);
        UserEntity savedEntity = userJpaRepository.save(entity);

        return UserResult.of(savedEntity.getId(), userMapper.toDomain(savedEntity));
    }
}