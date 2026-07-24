package com.gm.core.domain.user.model;

import java.util.List;

/**
 * AI가 자유텍스트에서 뽑아낸 음식 취향 키워드 원본. AI는 DB id를 모르며 이름만 반환한다.
 *
 * <p>좋아함/싫어함(극성)은 여기서 구분하지 않는다 — 어느 입력칸에서 왔는지 프론트가 알고,
 * 저장 시점에 확정한다. 카테고리 매칭 여부는 {@code FoodPreferenceExtractionService}가
 * 카테고리 마스터 화이트리스트로 판정한다.</p>
 *
 * @param foodKeywords 추출된 음식 취향 키워드 (카테고리 마스터 표기로 정규화 시도된 상태)
 */
public record ExtractedFoodPreference(
        List<String> foodKeywords
) {
}
