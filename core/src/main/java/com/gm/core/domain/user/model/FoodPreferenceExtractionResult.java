package com.gm.core.domain.user.model;

import java.util.List;

import com.gm.core.domain.menu.model.Menu;

/**
 * 화이트리스트 검증·UUID 매핑까지 끝난 음식 취향 추출 최종 결과.
 * 극성(LIKE/EXCLUDE)은 담지 않으며, 저장 시점에 matchedMenus는 user_menu_preference로,
 * unmatchedText는 preference_text/exclude_food_text로 갈라 저장한다.
 */
public record FoodPreferenceExtractionResult(
        List<Menu> matchedMenus,
        String unmatchedText
) {
}
