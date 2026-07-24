package com.gm.core.domain.menu.repository;

import java.util.List;

import com.gm.core.domain.menu.model.Category;

public interface CategoryRepository {

    /** 음식 카테고리 마스터 전체를 조회한다. */
    List<Category> findAll();
}
