package com.gm.core.domain.user.model;

import java.util.List;

import com.gm.core.domain.menu.model.Allergen;

/**
 * 화이트리스트 검증·UUID 매핑까지 끝난 알레르기 추출 최종 결과.
 *
 * @param standardAllergens 마스터(22종)와 매칭 확정된 표준 알레르기 (id 포함) — user_allergen에 저장
 * @param customAllergens 마스터에 없어 텍스트로만 보관할 비표준 알레르기 — user.custom_allergen_text
 */
public record AllergenExtractionResult(
        List<Allergen> standardAllergens,
        List<String> customAllergens
) {
}
