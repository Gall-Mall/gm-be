package com.gm.core.domain.user_setting.user_preference.user_category.service;

import com.gm.core.domain.user_setting.user_preference.user_category.model.UserCategory;
import com.gm.core.domain.user_setting.user_preference.user_category.repository.UserCategoryRepository;
import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCategoryService {
    /**
     * 유저가 선택한 카테고리를 조회
     * 유저가 선택한 카테고리를 삭제
     * 유저가 카테고리를 선택하면 선택카테고리를 user_category_preference에 추가
     */
    private final UserCategoryRepository userCategoryRepository;

    public List<UserCategory> getUserCategoryPreferences(UUID userId) {
        return userCategoryRepository.findByUserId(userId);
    }

    public void deleteUserCategoryPreferences(UUID userId) {
        userCategoryRepository.deleteByUserId(userId);
    }

    public void saveUserCategoryPreference(UUID userId, List<UUID> categoryIds, UserPreference preference) {
        userCategoryRepository.saveUserCategoryPreference(userId, categoryIds, preference);
    }
}
