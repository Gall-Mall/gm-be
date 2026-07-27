package com.gm.core.domain.user.model;

import java.util.List;

import com.gm.core.domain.menu.model.Category;
import com.gm.core.domain.menu.model.Menu;

/**
 * 화이트리스트 검증·UUID 매핑까지 끝난 음식 취향 추출 최종 결과.
 * 극성(LIKE/EXCLUDE)은 담지 않으며, 저장 시점에 matchedMenus는 user_menu_preference로,
 * matchedCategories는 user_category_preference로, unmatchedText는
 * preference_text/exclude_food_text로 갈라 저장한다.
 *
 * <p>"한식"처럼 카테고리 단위로 말한 취향은 메뉴 마스터에 없어 텍스트로만 남았고, 그 결과
 * 추천 스코어링의 카테고리 가중치(CAT_LIKE/CAT_DISLIKE)가 전혀 적용되지 않았다.
 * 카테고리 매칭을 분리해 id로 확정할 수 있는 취향은 구조화해 저장한다.</p>
 */
public record FoodPreferenceExtractionResult(
        List<Menu> matchedMenus,
        List<Category> matchedCategories,
        String unmatchedText
) {
}
