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

    /** 호출자가 전달한 동점 후보 목록을 외부에서 바꿀 수 없게 복사한다. */
    public FinalMenuVoteResult {
        tiedCandidateIds = tiedCandidateIds == null ? List.of() : List.copyOf(tiedCandidateIds);
    }

    /** 전체 그룹원의 응답을 기다리는 결과를 만든다. */
    public static FinalMenuVoteResult waiting() {
        return new FinalMenuVoteResult(Status.WAITING, null, List.of());
    }

    /** 단독 1위 후보가 자동 선택된 결과를 만든다. */
    public static FinalMenuVoteResult selected(UUID candidateId) {
        return new FinalMenuVoteResult(Status.SELECTED, candidateId, List.of());
    }

    /** 동점 후보를 방장 선택 대상으로 반환하는 결과를 만든다. */
    public static FinalMenuVoteResult tied(List<UUID> candidateIds) {
        return new FinalMenuVoteResult(Status.TIED, null, candidateIds);
    }

    public enum Status {
        WAITING,
        SELECTED,
        TIED
    }
}
