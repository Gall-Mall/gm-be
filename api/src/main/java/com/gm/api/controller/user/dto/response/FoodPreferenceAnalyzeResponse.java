package com.gm.api.controller.user.dto.response;

import java.util.List;
import java.util.UUID;

import com.gm.core.domain.user.model.FoodPreferenceExtractionResult;

/**
 * 자유텍스트 음식 취향 분석 결과. 사용자가 확인·수정한 뒤 온보딩 제출에서 확정 저장한다.
 * FE는 matchedMenus를 선택 상태로 반영하고, unmatchedText는 preference/exclude_food_text로 저장한다.
 */
public record FoodPreferenceAnalyzeResponse(
        List<MenuResponse> matchedMenus,
        String unmatchedText
) {

    public record MenuResponse(UUID id, String name) {
    }

    public static FoodPreferenceAnalyzeResponse from(FoodPreferenceExtractionResult result) {
        return new FoodPreferenceAnalyzeResponse(
                result.matchedMenus().stream()
                        .map(m -> new MenuResponse(m.id(), m.name()))
                        .toList(),
                result.unmatchedText()
        );
    }
}
