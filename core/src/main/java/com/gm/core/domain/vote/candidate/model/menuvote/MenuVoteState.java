package com.gm.core.domain.vote.candidate.model.menuvote;

import java.time.Instant;
import java.util.List;

/** Redis에 보관된 1차 메뉴 투표의 현재 상태와 후보별 최신 집계다. */
public record MenuVoteState(
        Status status,
        Instant deadline,
        List<MenuVoteCount> counts
) {
    /** 진행 상태의 필수 값과 후보별 집계를 확인한다. */
    public MenuVoteState {
        counts = counts == null ? List.of() : List.copyOf(counts);
    }

    public enum Status {
        OPEN,
        CLOSED
    }
}
