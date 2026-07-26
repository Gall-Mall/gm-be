package com.gm.core.domain.recommendation.model;

import java.util.List;

/**
 * 그룹 멤버들이 자유텍스트로 남긴 신호.
 *
 * <p>allergenTexts는 하드 제외 지시, 나머지 둘은 AI 큐레이션의 소프트 신호로 쓰인다.</p>
 */
public record GroupSoftSignals(
        List<String> allergenTexts,
        List<String> preferenceTexts,
        List<String> excludeFoodTexts
) {
    public static GroupSoftSignals empty() {
        return new GroupSoftSignals(List.of(), List.of(), List.of());
    }
}
