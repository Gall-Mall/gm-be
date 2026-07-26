package com.gm.api.controller.vote.candidate.dto.response;

import java.util.UUID;

import com.gm.core.domain.vote.candidate.model.VoteCandidate;

/** 최종 메뉴 확정 결과다. */
public record FinalMenuSelectionResponse(UUID selectedCandidateId, UUID menuId) {
    public static FinalMenuSelectionResponse from(VoteCandidate candidate) {
        return new FinalMenuSelectionResponse(candidate.id(), candidate.menuId());
    }
}
