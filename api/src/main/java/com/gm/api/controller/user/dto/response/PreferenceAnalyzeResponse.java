package com.gm.api.controller.user.dto.response;

import java.util.List;
import java.util.UUID;

import com.gm.core.domain.user.model.PreferenceExtractionResult;

/**
 * 자유텍스트 선호 분석 결과. 사용자가 파싱 결과를 확인·수정한 뒤 확정 저장한다.
 *
 * @param standardAllergens 표준 알레르기 마스터와 매칭 확정된 항목
 * @param customAllergens 표준에 없어 자유텍스트로 보관될 비표준 알레르기
 * @param preferenceText 선호 음식 요약
 * @param excludeFoodText 싫어하는 음식 요약
 */
public record PreferenceAnalyzeResponse(
        List<AllergenResponse> standardAllergens,
        List<String> customAllergens,
        String preferenceText,
        String excludeFoodText
) {

    public record AllergenResponse(UUID id, String name) {
    }

    public static PreferenceAnalyzeResponse from(PreferenceExtractionResult result) {
        return new PreferenceAnalyzeResponse(
                result.standardAllergens().stream()
                        .map(a -> new AllergenResponse(a.id(), a.name()))
                        .toList(),
                result.customAllergens(),
                result.preferenceText(),
                result.excludeFoodText()
        );
    }
}
