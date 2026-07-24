package com.gm.core.domain.recommendation.model;

/**
 * AI가 큐레이션한 메뉴 원본. AI는 DB id를 모르며 후보 이름과 추천 이유만 반환한다.
 * {@code RecommendationCurationService}가 후보 화이트리스트로 이름→UUID를 매핑한 뒤에만 저장에 쓴다.
 */
public record CuratedMenu(
        String menuName,
        String description
) {
}
