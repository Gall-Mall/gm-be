package com.gm.core.domain.vote.candidate.model;

import java.util.UUID;

/** deadline 기준 최종투표 원자 마감 결과다. */
public record FinalMenuVoteCloseResult(Status status, UUID selectedCandidateId) {

    /** 아직 마감 시각이 되지 않은 결과를 만든다. */
    public static FinalMenuVoteCloseResult notDue() {
        return new FinalMenuVoteCloseResult(Status.NOT_DUE, null);
    }

    /** Redis 최종 투표 상태를 찾지 못한 결과를 만든다. */
    public static FinalMenuVoteCloseResult notFound() {
        return new FinalMenuVoteCloseResult(Status.NOT_FOUND, null);
    }

    /** 단독 1위 후보가 결정된 결과를 만든다. */
    public static FinalMenuVoteCloseResult uniqueWinner(UUID candidateId) {
        return new FinalMenuVoteCloseResult(Status.UNIQUE_WINNER, candidateId);
    }

    /** 동점 또는 무응답으로 방장 선택이 필요한 결과를 만든다. */
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
