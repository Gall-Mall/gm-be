package com.gm.core.domain.user_preference.user_category;

/**
 * 카테고리 선호 종류.
 *
 * 메뉴({@code UserPreference})가 콕 집어 제외하는 하드 성격(LIKE/EXCLUDE)인 것과 달리,
 * 카테고리는 소프트 취향(LIKE/DISLIKE)이다. DISLIKE는 완전 제외가 아니라 점수 감점으로만 작용한다.
 */
public enum CategoryPreference {
    LIKE, DISLIKE
}
