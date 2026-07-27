package com.gm.core.domain.vote.candidate.model.menuvote;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Redis에 보관된 1차 메뉴 투표의 현재 상태와 후보별 최신 집계다. */
public record MenuVoteState(
        Status status,
        Instant deadline,
        List<MenuVoteCount> counts,
        List<UUID> completedUserIds
) {
    /** 진행 상태의 필수 값과 후보별 집계를 확인한다. */
    public MenuVoteState {
        counts = counts == null ? List.of() : List.copyOf(counts);
        completedUserIds = completedUserIds == null ? List.of() : List.copyOf(completedUserIds);
    }

    /**
     * 완료 사용자 목록이 없는 기존 호출을 빈 목록으로 변환한다.
     *
     * @param status 투표 상태
     * @param deadline 투표 마감 시각
     * @param counts 후보별 집계
     */
    public MenuVoteState(Status status, Instant deadline, List<MenuVoteCount> counts) {
        this(status, deadline, counts, List.of());
    }

    public enum Status {
        OPEN,
        CLOSED
    }
}
