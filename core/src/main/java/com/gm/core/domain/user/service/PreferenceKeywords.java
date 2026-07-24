package com.gm.core.domain.user.service;

import java.util.List;
import java.util.Objects;

/**
 * AI가 반환한 선호/알레르기 이름을 저장·응답 전에 정제한다.
 *
 * 비표준 알레르기·잔여 취향은 콤마 구분 단일 컬럼(VARCHAR(500))에 저장되므로,
 * 개별 항목에서 콤마(구분자 충돌)와 HTML 위험 문자를 제거한다.
 * 알레르기·음식 두 서비스가 동일 정책을 쓰도록 여기서 강제한다.
 */
final class PreferenceKeywords {

    private PreferenceKeywords() {
    }

    /** 저장 구분자(콤마)와 HTML 위험 문자를 공백으로 치환하고 공백을 정리한다. */
    static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[<>&\",]", " ").replaceAll("\\s+", " ").trim();
    }

    /** AI 반환 목록을 정제한다: null 제거 → 위험문자 제거 → 공백/길이 필터 → 중복 제거. */
    static List<String> cleaned(List<String> raw, int maxNameLength) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .filter(Objects::nonNull)
                .map(PreferenceKeywords::sanitize)
                .filter(name -> !name.isEmpty() && name.length() <= maxNameLength)
                .distinct()
                .toList();
    }
}
