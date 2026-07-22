package com.gm.db.domain.user_preference.user_allergen.repository;

import com.gm.core.domain.user_setting.user_preference.user_allergen.model.UserAllergen;
import com.gm.core.domain.user_setting.user_preference.user_allergen.repository.UserAllergenRepository;
import com.gm.db.domain.user_preference.user_allergen.entity.UserAllergenEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserAllergenRepositoryImpl implements UserAllergenRepository {

    private final UserAllergenJpaRepository userAllergenJpaRepository;

    @Override
    public void deleteByUserId(UUID userId) {
        userAllergenJpaRepository.deleteByUserId(userId);
        userAllergenJpaRepository.flush();
    }

    @Override
    public void addUserAllergens(UUID userId, List<UUID> allergenIds) {
        userAllergenJpaRepository.saveAll(allergenIds.stream().map(i -> new UserAllergenEntity(userId, i)).toList());
    }

    @Override
    public List<UserAllergen> findByUserId(UUID userId) {
        return userAllergenJpaRepository.findByUserId(userId);
    }
}
