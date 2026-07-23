package com.gm.db.domain.user.preference.user_category.repository;

import com.gm.core.domain.user.exception.UserErrorCode;
import com.gm.core.domain.user.exception.UserException;
import com.gm.core.domain.user.model.UserCategory;
import com.gm.core.domain.user.repository.UserCategoryRepository;
import com.gm.core.domain.user.model.UserPreference;
import com.gm.db.domain.menu.category.entity.FoodCategoryEntity;
import com.gm.db.domain.menu.category.repository.FoodCategoryJpaRepository;
import com.gm.db.domain.user.preference.user_category.mapper.UserCategoryMapper;
import com.gm.db.domain.user.preference.user_category.entity.UserCategoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserCategoryRepositoryImpl implements UserCategoryRepository {

    private final UserCategoryJpaRepository userCategoryJpaRepository;
    private final FoodCategoryJpaRepository foodCategoryJpaRepository;
    private final UserCategoryMapper userCategoryMapper;

    @Override
    public List<UserCategory> findByUserId(UUID userId) {
        return userCategoryJpaRepository.findByUserId(userId).stream().map(userCategoryMapper::toDomain).toList();
    }

    @Override
    public void deleteByUserId(UUID userId) {
        userCategoryJpaRepository.deleteByUserId(userId);
        userCategoryJpaRepository.flush();
    }

    @Override
    public void saveUserCategoryPreference(UUID userId, List<UUID> categoryIds, UserPreference preference) {
        for (UUID uuid : categoryIds) {
            FoodCategoryEntity foodCategoryEntity = foodCategoryJpaRepository.findById(uuid)
                    .orElseThrow(() -> new UserException(UserErrorCode.CATEGORY_NOT_FOUND));
            userCategoryJpaRepository.save(new UserCategoryEntity(userId, foodCategoryEntity.getId(), preference));
        }
    }
}
