package com.gm.core.domain.vote.candidate.model;

import java.util.UUID;

import org.springframework.util.Assert;

/**
 * 추천 기능이 생성한 메뉴 후보 입력이다.
 *
 * @param menuId 메뉴 식별자
 * @param displayOrder 투표 화면 노출 순서
 * @param description 추천 이유
 */
public record RecommendedMenuCandidate(
        UUID menuId,
        int displayOrder,
        String description
) {

    /**
     * 메뉴 식별자와 노출 순서가 올바른지 확인한다.
     *
     * @throws IllegalArgumentException 메뉴 식별자가 없거나 노출 순서가 1보다 작은 경우
     */
    public RecommendedMenuCandidate {
        Assert.notNull(menuId, "menuId must not be null");
        Assert.isTrue(displayOrder > 0, "displayOrder must be positive");
    }
}
