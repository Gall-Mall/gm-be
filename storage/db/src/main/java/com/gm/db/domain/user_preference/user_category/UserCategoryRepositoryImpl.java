package com.gm.db.domain.user_preference.user_category;

import com.gm.core.domain.user_setting.user_preference.user_category.model.UserCategory;
import com.gm.core.domain.user_setting.exception.UserSettingErrorCode;
import com.gm.core.domain.user_setting.exception.UserSettingException;
import com.gm.core.domain.user_setting.user_preference.user_category.repository.UserCategoryRepository;
import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserPreference;
import com.gm.db.domain.menu.category.FoodCategoryEntity;
import com.gm.db.domain.menu.category.FoodCategoryJpaRepository;
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
                    .orElseThrow(() -> new UserSettingException(UserSettingErrorCode.CATEGORY_NOT_FOUND));
            userCategoryJpaRepository.save(new UserCategoryEntity(userId, foodCategoryEntity.getId(), preference));
        }
    }
}
