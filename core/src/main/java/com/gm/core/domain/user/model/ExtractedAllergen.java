package com.gm.core.domain.user.model;

import java.util.List;

/**
 * AI가 자유텍스트에서 뽑아낸 알레르기 키워드 원본. AI는 DB id를 모르며 이름만 반환한다.
 *
 * <p>표준/비표준 분류는 여기서 하지 않는다. AI는 알레르기로 보이는 키워드를 모두 담고,
 * 마스터에 있는 이름은 그 표기 그대로 정규화만 한다. 표준/비표준 판정은
 * {@code AllergenExtractionService}가 마스터 화이트리스트로 수행한다.</p>
 *
 * @param allergenNames 자유텍스트에서 추출된 알레르기 키워드 (마스터 표기로 정규화 시도된 상태)
 */
public record ExtractedAllergen(
        List<String> allergenNames
) {
}
