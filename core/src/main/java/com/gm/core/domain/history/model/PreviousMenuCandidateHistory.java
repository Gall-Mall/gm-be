package com.gm.core.domain.history.model;

import java.util.UUID;

import com.gm.core.domain.vote.candidate.model.menu.VoteCandidateResult;

/** 완료된 투표 세션의 메뉴 후보와 최종 집계 스냅샷이다. */
public record PreviousMenuCandidateHistory(
        UUID menuId,
        String name,
        String imageUrl,
        int displayOrder,
        boolean selected,
        int goCount,
        int maybeCount,
        int noCount,
        int respondentCount,
        VoteCandidateResult resultStatus
) {
}
