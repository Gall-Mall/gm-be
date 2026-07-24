package com.gm.db.domain.menu.category.repository;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.menu.model.Category;
import com.gm.core.domain.menu.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

/**
 * 음식 카테고리 마스터 조회 구현체.
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryRepositoryImpl implements CategoryRepository {

    private final FoodCategoryJpaRepository foodCategoryJpaRepository;

    @Override
    public List<Category> findAll() {
        return foodCategoryJpaRepository.findAll().stream()
                .map(entity -> new Category(entity.getId(), entity.getName()))
                .toList();
    }
}
