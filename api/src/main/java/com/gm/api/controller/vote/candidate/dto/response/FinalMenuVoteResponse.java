package com.gm.api.controller.vote.candidate.dto.response;

import java.util.List;
import java.util.UUID;

import com.gm.core.domain.vote.candidate.model.finalmenu.FinalMenuVoteResult;

/** 두 후보 최종 투표의 현재 결과다. */
public record FinalMenuVoteResponse(
        FinalMenuVoteResult.Status status,
        UUID selectedCandidateId,
        List<UUID> tiedCandidateIds
) {
    /** 최종 투표 처리 결과를 API 응답으로 변환한다. */
    public static FinalMenuVoteResponse from(FinalMenuVoteResult result) {
        return new FinalMenuVoteResponse(
                result.status(),
                result.selectedCandidateId(),
                result.tiedCandidateIds()
        );
    }
}
