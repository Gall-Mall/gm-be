package com.gm.core.domain.user.port;

import java.util.List;

import com.gm.core.domain.user.model.ExtractedFoodPreference;

/**
 * 자유텍스트 음식 취향 추출을 외부 AI에 위임하는 포트.
 *
 * 보안 제약: AI는 DB에 접근하지 않는다. 앱이 메뉴 이름 목록을 함께 넘기고,
 * AI는 사용자가 말한 구체적 음식을 그대로 추출하되 목록에 있는 이름은 그 표기로 정규화한다.
 * (상위 카테고리로 일반화하지 않는다.) 메뉴 매칭 최종 판정은 코드가 한다.
 */
public interface FoodPreferenceAiPort {

    /** 자유텍스트에서 음식 취향 키워드를 추출한다. menuNames는 정규화 기준으로 넘긴다. */
    ExtractedFoodPreference extractFoodPreference(String freeText, List<String> menuNames);
}
