package com.gm.core.domain.user.service;

import com.gm.core.domain.user.model.UserCategory;
import com.gm.core.domain.user.model.UserCategoryPreference;
import com.gm.core.domain.user.repository.UserCategoryRepository;
import com.gm.core.domain.user.model.UserMenuPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCategoryService {

    private final UserCategoryRepository userCategoryRepository;

    /**
     * 유저가 선택한 카테고리를 조회
     */
    public List<UserCategory> getUserCategoryPreferences(UUID userId) {
        return userCategoryRepository.findByUserId(userId);
    }

    /**
     * 유저가 선택한 카테고리를 삭제
     */
    public void deleteUserCategoryPreferences(UUID userId) {
        userCategoryRepository.deleteByUserId(userId);
    }

    /**
     * 유저가 카테고리를 선택하면 선택카테고리를 user_category_preference에 추가
     */
    public void saveUserCategoryPreference(UUID userId, List<UUID> preferredCategoryIds, List<UUID> disLikeCategoryIds) {
        userCategoryRepository.saveUserCategoryPreference(userId, preferredCategoryIds, disLikeCategoryIds);
    }
}
