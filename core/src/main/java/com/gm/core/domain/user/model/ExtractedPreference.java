package com.gm.core.domain.user.model;

import java.util.List;

/**
 * AI가 자유텍스트에서 추출한 원본 결과. AI는 DB id를 모르며 이름만 반환한다.
 *
 * <p>검증 전 상태이므로 그대로 저장하면 안 된다.
 * {@code PreferenceExtractionService}가 화이트리스트 검증·UUID 매핑을 거친 뒤에만 사용한다.</p>
 *
 * @param standardAllergens 제공된 표준 알레르기 목록 중 매칭된 이름들
 * @param customAllergens 표준 목록에 없는 비표준 알레르기 표현들 (user.custom_allergen_text)
 * @param preferenceText 선호 음식 자유텍스트 (user.preference_text)
 * @param excludeFoodText 싫어하는 음식 자유텍스트 (user.exclude_food_text)
 */
public record ExtractedPreference(
        List<String> standardAllergens,
        List<String> customAllergens,
        String preferenceText,
        String excludeFoodText
) {
}
