package com.gm.api.controller.user.dto.response;

import java.util.List;
import java.util.UUID;

import com.gm.core.domain.user.model.FoodPreferenceExtractionResult;

/**
 * 자유텍스트 음식 취향 분석 결과. 사용자가 확인·수정한 뒤 온보딩 제출에서 확정 저장한다.
 *
 * @param matchedCategories 카테고리 마스터와 매칭된 항목 — FE에서 해당 카테고리를 선택 상태로 반영
 * @param unmatchedText 카테고리로 분류되지 않은 취향 텍스트 — 저장 시 preference/exclude_food_text
 */
public record FoodPreferenceAnalyzeResponse(
        List<CategoryResponse> matchedCategories,
        String unmatchedText
) {

    public record CategoryResponse(UUID id, String name) {
    }

    public static FoodPreferenceAnalyzeResponse from(FoodPreferenceExtractionResult result) {
        return new FoodPreferenceAnalyzeResponse(
                result.matchedCategories().stream()
                        .map(c -> new CategoryResponse(c.id(), c.name()))
                        .toList(),
                result.unmatchedText()
        );
    }
}
