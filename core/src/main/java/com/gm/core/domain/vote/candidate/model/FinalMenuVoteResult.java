package com.gm.core.domain.vote.candidate.model;

import java.util.List;
import java.util.UUID;

/**
 * 두 후보 최종 투표의 원자 처리 결과다.
 *
 * @param status 투표 진행 상태
 * @param selectedCandidateId 단독 1위로 자동 확정할 후보
 * @param tiedCandidateIds 전원 응답 후 동점인 후보
 */
public record FinalMenuVoteResult(
        Status status,
        UUID selectedCandidateId,
        List<UUID> tiedCandidateIds
) {

    public FinalMenuVoteResult {
        tiedCandidateIds = tiedCandidateIds == null ? List.of() : List.copyOf(tiedCandidateIds);
    }

    public static FinalMenuVoteResult waiting() {
        return new FinalMenuVoteResult(Status.WAITING, null, List.of());
    }

    public static FinalMenuVoteResult selected(UUID candidateId) {
        return new FinalMenuVoteResult(Status.SELECTED, candidateId, List.of());
    }

    public static FinalMenuVoteResult tied(List<UUID> candidateIds) {
        return new FinalMenuVoteResult(Status.TIED, null, candidateIds);
    }

    public enum Status {
        WAITING,
        SELECTED,
        TIED
    }
}
