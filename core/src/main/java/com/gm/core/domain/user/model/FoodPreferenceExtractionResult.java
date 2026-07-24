package com.gm.core.domain.user.model;

import java.util.List;

import com.gm.core.domain.menu.model.Category;

/**
 * 화이트리스트 검증·UUID 매핑까지 끝난 음식 취향 추출 최종 결과.
 *
 * <p>극성(LIKE/DISLIKE)은 담지 않는다. 저장 시점(온보딩 제출)에 어느 입력칸이었는지에 따라
 * user_category_preference와 preference_text/exclude_food_text로 갈라 저장한다.</p>
 *
 * @param matchedCategories 카테고리 마스터와 매칭된 항목 (id 포함) — user_category_preference에 저장
 * @param unmatchedText 카테고리로 분류되지 않은 취향 텍스트 — preference_text 또는 exclude_food_text
 */
public record FoodPreferenceExtractionResult(
        List<Category> matchedCategories,
        String unmatchedText
) {
}
