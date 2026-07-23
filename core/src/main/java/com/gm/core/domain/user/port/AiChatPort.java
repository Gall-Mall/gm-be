package com.gm.core.domain.user.port;

import java.util.List;

import com.gm.core.domain.user.model.ExtractedPreference;

/**
 * 자유텍스트 선호/알레르기 추출을 외부 AI에 위임하는 포트.
 *
 * <p>보안 제약: AI는 DB에 접근하지 않는다. 앱이 표준 알레르기 이름 목록을 함께 넘기고,
 * AI는 그 목록 안의 이름만 반환한다. 최종 판정(화이트리스트 검증·id 매핑)은 코드가 한다.</p>
 */
public interface AiChatPort {

    /**
     * 자유텍스트에서 알레르기·호불호 신호를 추출한다.
     *
     * @param freeText 사용자가 입력한 자유텍스트
     * @param standardAllergenNames 매칭 후보로 제공할 표준 알레르기 이름 목록
     * @return 검증 전의 AI 추출 원본
     */
    ExtractedPreference extractPreference(String freeText, List<String> standardAllergenNames);
}
