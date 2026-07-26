package com.gm.core.domain.vote.candidate.model;

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
    public FinalMenuVoteState {
        counts = counts == null ? List.of() : List.copyOf(counts);
    }

    public enum Status {
        OPEN,
        SELECTED,
        OWNER_SELECTION_PENDING
    }
}
