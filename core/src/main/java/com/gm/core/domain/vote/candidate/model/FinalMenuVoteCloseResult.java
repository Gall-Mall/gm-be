package com.gm.core.domain.vote.candidate.model;

import java.util.UUID;

/** deadline 기준 최종투표 원자 마감 결과다. */
public record FinalMenuVoteCloseResult(Status status, UUID selectedCandidateId) {

    public static FinalMenuVoteCloseResult notDue() {
        return new FinalMenuVoteCloseResult(Status.NOT_DUE, null);
    }

    public static FinalMenuVoteCloseResult notFound() {
        return new FinalMenuVoteCloseResult(Status.NOT_FOUND, null);
    }

    public static FinalMenuVoteCloseResult uniqueWinner(UUID candidateId) {
        return new FinalMenuVoteCloseResult(Status.UNIQUE_WINNER, candidateId);
    }

    public static FinalMenuVoteCloseResult ownerSelectionPending() {
        return new FinalMenuVoteCloseResult(Status.OWNER_SELECTION_PENDING, null);
    }

    public enum Status {
        NOT_DUE,
        NOT_FOUND,
        UNIQUE_WINNER,
        OWNER_SELECTION_PENDING
    }
}
