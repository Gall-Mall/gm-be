package com.gm.core.domain.vote.candidate.model.finalmenu;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Redis에 보관된 두 후보 최종투표의 현재 상태다. */
public record FinalMenuVoteState(
        Status status,
        Instant deadline,
        List<FinalMenuVoteCount> counts,
        int respondedCount,
        UUID selectedCandidateId
) {
    /** 최종 투표 상태의 필수 값과 집계를 확인한다. */
    public FinalMenuVoteState {
        counts = counts == null ? List.of() : List.copyOf(counts);
    }

    public enum Status {
        OPEN,
        SELECTED,
        OWNER_SELECTION_PENDING
    }
}
