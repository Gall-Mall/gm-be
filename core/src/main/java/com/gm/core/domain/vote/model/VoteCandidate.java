package com.gm.core.domain.vote.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 투표 세션에 노출된 메뉴 후보와 최종 집계 결과를 보관한다.
 *
 * @param id 후보 식별자
 * @param voteSessionId 투표 세션 식별자
 * @param menuId 메뉴 식별자
 * @param displayOrder 세션 내 노출 순서
 * @param selected 최종 메뉴 선택 여부
 * @param goCount 갈래 응답 수
 * @param maybeCount 애매 응답 수
 * @param noGoCount 말래 응답 수
 * @param respondentCount 응답자 수
 * @param resultStatus 투표 판정 상태
 * @param description 추천 이유
 * @param createdAt 생성 시각
 * @param updatedAt 수정 시각
 */
@Builder(toBuilder = true)
public record VoteCandidate(
        UUID id,
        UUID voteSessionId,
        UUID menuId,
        int displayOrder,
        boolean selected,
        Integer goCount,
        Integer maybeCount,
        Integer noGoCount,
        Integer respondentCount,
        VoteCandidateResult resultStatus,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
