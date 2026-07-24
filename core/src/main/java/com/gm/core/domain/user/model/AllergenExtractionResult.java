package com.gm.core.domain.user.model;

import java.util.List;

import com.gm.core.domain.menu.model.Allergen;

/**
 * 화이트리스트 검증·UUID 매핑까지 끝난 알레르기 추출 최종 결과.
 * standardAllergens는 user_allergen(id)에, customAllergens는 user.custom_allergen_text에 저장된다.
 */
public record AllergenExtractionResult(
        List<Allergen> standardAllergens,
        List<String> customAllergens
) {
}
