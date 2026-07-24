package com.gm.api.controller.vote.candidate.dto.response;

import java.util.UUID;

import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;

/**
 * 메뉴 투표 화면에 표시할 후보 정보이다.
 *
 * @param voteCandidateId 메뉴 후보 식별자
 * @param menuId 메뉴 식별자
 * @param categoryId 카테고리 식별자
 * @param menuName 메뉴 이름
 * @param categoryName 카테고리 이름
 * @param imageUrl 메뉴 이미지 주소
 * @param displayOrder 노출 순서
 * @param counts 갈래·애매·말래 집계
 * @param respondentCount 응답자 수
 * @param resultStatus 후보 판정 상태
 * @param description 추천 이유
 */
public record MenuCandidateResponse(
        UUID voteCandidateId,
        UUID menuId,
        UUID categoryId,
        String menuName,
        String categoryName,
        String imageUrl,
        int displayOrder,
        VoteCountsResponse counts,
        int respondentCount,
        VoteCandidateResult resultStatus,
        String description
) {

    /**
     * 메뉴 후보 도메인을 API 응답으로 변환한다.
     *
     * @param candidate 변환할 메뉴 후보
     * @return 메뉴 투표 화면 응답
     */
    public static MenuCandidateResponse from(MenuVoteCandidate candidate) {
        return new MenuCandidateResponse(
                candidate.voteCandidateId(),
                candidate.menuId(),
                candidate.categoryId(),
                candidate.menuName(),
                candidate.categoryName(),
                candidate.imageUrl(),
                candidate.displayOrder(),
                new VoteCountsResponse(
                        candidate.goCount(),
                        candidate.maybeCount(),
                        candidate.noCount()
                ),
                candidate.respondentCount(),
                candidate.resultStatus(),
                candidate.description()
        );
    }

    /**
     * 메뉴 후보의 최신 투표 집계이다.
     *
     * @param go 갈래 응답 수
     * @param maybe 애매 응답 수
     * @param no 말래 응답 수
     */
    public record VoteCountsResponse(
            int go,
            int maybe,
            int no
    ) {
    }
}
