package com.gm.core.domain.recommendation.model;

import java.util.List;

/**
 * AI 메뉴 큐레이션 요청. AI에 넘길 이름·텍스트만 담는다(DB id 없음).
 * allergenExclusionTexts는 하드 제외 지시, preferenceTexts·excludeFoodTexts는 소프트 신호로 쓰인다.
 */
public record MenuCurationCommand(
        List<String> candidateMenuNames,
        List<String> allergenExclusionTexts,
        List<String> preferenceTexts,
        List<String> excludeFoodTexts,
        int maxCount
) {
}
