package com.gm.core.domain.user.port;

import java.util.List;

import com.gm.core.domain.user.model.ExtractedAllergen;

/**
 * 자유텍스트 알레르기 추출을 외부 AI에 위임하는 포트.
 *
 * 보안 제약: AI는 DB에 접근하지 않는다. 앱이 표준 알레르기 이름 목록을 함께 넘기고,
 * AI는 알레르기 키워드를 추출하되 목록에 있는 이름은 그 표기 그대로 정규화한다.
 * 표준/비표준 최종 판정(화이트리스트 검증·id 매핑)은 코드가 한다.
 */
public interface AiChatPort {

    /** 자유텍스트에서 알레르기 키워드를 추출한다. standardAllergenNames는 정규화 기준으로 넘긴다. */
    ExtractedAllergen extractAllergens(String freeText, List<String> standardAllergenNames);
}
