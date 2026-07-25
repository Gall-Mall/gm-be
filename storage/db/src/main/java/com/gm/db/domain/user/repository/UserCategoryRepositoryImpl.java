package com.gm.db.domain.user.repository;

import com.gm.core.domain.user.model.UserCategory;
import com.gm.core.domain.user.model.UserCategoryPreference;
import com.gm.core.domain.user.repository.UserCategoryRepository;
import com.gm.db.domain.user.mapper.UserCategoryMapper;
import com.gm.db.domain.user.entity.UserCategoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
public class UserCategoryRepositoryImpl implements UserCategoryRepository {

    private final UserCategoryJpaRepository userCategoryJpaRepository;
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
    public void saveUserCategoryPreference(UUID userId, List<UUID> preferredCategoryIds, List<UUID> disLikeCategoryIds) {

        List<UserCategoryEntity> userCategoryEntities = Stream.concat(
                preferredCategoryIds.stream()
                        .map(categoryId -> new UserCategoryEntity(
                                userId,
                                categoryId,
                                UserCategoryPreference.LIKE
                        )),

                disLikeCategoryIds.stream()
                        .map(categoryId -> new UserCategoryEntity(
                                userId,
                                categoryId,
                                UserCategoryPreference.DISLIKE
                        ))
        ).toList();


        userCategoryJpaRepository.saveAll(userCategoryEntities);
    }
}
