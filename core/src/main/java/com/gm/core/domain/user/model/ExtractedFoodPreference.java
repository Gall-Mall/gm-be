package com.gm.core.domain.user.model;

import java.util.List;

/**
 * AI가 자유텍스트에서 뽑아낸 음식 취향 키워드 원본. AI는 DB id를 모르며 이름만 반환한다.
 * 좋아함/싫어함(극성)은 구분하지 않고, 메뉴 매칭은 {@code FoodPreferenceExtractionService}가
 * 메뉴 마스터 화이트리스트로 판정한다.
 */
public record ExtractedFoodPreference(
        List<String> foodKeywords
) {
}
