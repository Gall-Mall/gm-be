package com.gm.core.domain.user.model;

import java.util.List;

import com.gm.core.domain.menu.model.Allergen;

/**
 * 화이트리스트 검증·UUID 매핑까지 끝난 선호 추출 최종 결과.
 *
 * @param standardAllergens 마스터와 매칭 확정된 표준 알레르기 (id 포함)
 * @param customAllergens 표준 목록에 없어 텍스트로만 보관할 비표준 알레르기
 * @param preferenceText 선호 음식 자유텍스트
 * @param excludeFoodText 싫어하는 음식 자유텍스트
 */
public record PreferenceExtractionResult(
        List<Allergen> standardAllergens,
        List<String> customAllergens,
        String preferenceText,
        String excludeFoodText
) {
}
