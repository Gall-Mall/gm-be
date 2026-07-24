package com.gm.core.domain.user.model;

import java.util.List;

/**
 * AI가 자유텍스트에서 뽑아낸 알레르기 키워드 원본. AI는 DB id를 모르며 이름만 반환한다.
 * 표준/비표준 판정은 하지 않는다 — {@code AllergenExtractionService}가 마스터 화이트리스트로 수행한다.
 */
public record ExtractedAllergen(
        List<String> allergenNames
) {
}
