package com.gm.api.controller.user.dto.response;

import java.util.List;
import java.util.UUID;

import com.gm.core.domain.user.model.FoodPreferenceExtractionResult;

/**
 * 자유텍스트 음식 취향 분석 결과. 사용자가 확인·수정한 뒤 온보딩 제출에서 확정 저장한다.
 * FE는 matchedMenus를 preferred/excludedMenuIds로, matchedCategories를
 * preferred/excludedCategoryIds로 나눠 보내고, unmatchedText는
 * preference_text/exclude_food_text로 저장된다.
 */
public record FoodPreferenceAnalyzeResponse(
        List<MenuResponse> matchedMenus,
        List<CategoryResponse> matchedCategories,
        String unmatchedText
) {

    public record MenuResponse(UUID id, String name) {
    }

    public record CategoryResponse(UUID id, String name) {
    }

    public static FoodPreferenceAnalyzeResponse from(FoodPreferenceExtractionResult result) {
        return new FoodPreferenceAnalyzeResponse(
                result.matchedMenus().stream()
                        .map(m -> new MenuResponse(m.id(), m.name()))
                        .toList(),
                result.matchedCategories().stream()
                        .map(c -> new CategoryResponse(c.id(), c.name()))
                        .toList(),
                result.unmatchedText()
        );
    }
}
