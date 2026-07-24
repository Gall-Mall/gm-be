package com.gm.core.domain.user.repository;

import com.gm.core.domain.user.model.UserCategory;
import com.gm.core.domain.user.model.UserCategoryPreference;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserCategoryRepository {
    List<UserCategory> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    void saveUserCategoryPreference(UUID userId, List<UUID> preferredCategoryIds, List<UUID> disLikeCategoryIds);
}
