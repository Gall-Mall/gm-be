package com.gm.core.domain.vote.candidate.model;

import java.util.UUID;

/**
 * 메뉴 투표 화면에 필요한 후보와 메뉴 정보이다.
 *
 * @param voteCandidateId 후보 식별자
 * @param voteSessionId 투표 세션 식별자
 * @param menuId 메뉴 식별자
 * @param categoryId 카테고리 식별자
 * @param menuName 메뉴 이름
 * @param categoryName 카테고리 이름
 * @param imageUrl 메뉴 이미지 주소
 * @param displayOrder 노출 순서
 * @param goCount 갈래 수
 * @param maybeCount 애매 수
 * @param noCount 말래 수
 * @param respondentCount 응답자 수
 * @param resultStatus 후보 판정 상태
 * @param description 추천 이유
 */
public record MenuVoteCandidate(
        UUID voteCandidateId,
        UUID voteSessionId,
        UUID menuId,
        UUID categoryId,
        String menuName,
        String categoryName,
        String imageUrl,
        int displayOrder,
        int goCount,
        int maybeCount,
        int noCount,
        int respondentCount,
        VoteCandidateResult resultStatus,
        String description
) {
}
