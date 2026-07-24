package com.gm.core.domain.user.port;

import java.util.List;

import com.gm.core.domain.user.model.ExtractedFoodPreference;

/**
 * 자유텍스트 음식 취향 추출을 외부 AI에 위임하는 포트.
 *
 * <p>보안 제약: AI는 DB에 접근하지 않는다. 앱이 카테고리 이름 목록을 함께 넘기고,
 * AI는 취향 키워드를 추출하되 목록에 있는 이름은 그 표기 그대로 정규화한다.
 * 카테고리 매칭 최종 판정은 코드가 한다.</p>
 */
public interface FoodPreferenceAiPort {

    /**
     * 자유텍스트에서 음식 취향 키워드를 추출한다.
     *
     * @param freeText 사용자가 입력한 자유텍스트
     * @param categoryNames 정규화 기준으로 제공할 음식 카테고리 이름 목록
     * @return 추출된 취향 키워드 원본 (검증 전)
     */
    ExtractedFoodPreference extractFoodPreference(String freeText, List<String> categoryNames);
}
