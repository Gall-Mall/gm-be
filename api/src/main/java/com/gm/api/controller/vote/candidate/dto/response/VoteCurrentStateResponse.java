package com.gm.api.controller.vote.candidate.dto.response;

import java.util.List;

import com.gm.core.domain.vote.candidate.model.FinalMenuVoteState;
import com.gm.core.domain.vote.candidate.model.MenuVoteState;
import com.gm.core.domain.vote.candidate.model.VoteCurrentState;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;

/** 화면 진입 또는 Socket 재연결 직후 동기화할 투표 세션 기준 상태다. */
public record VoteCurrentStateResponse(
        VoteSessionStatus sessionStatus,
        List<MenuCandidateResponse> candidates,
        MenuVoteState menuVote,
        FinalMenuVoteState finalMenuVote,
        FinalMenuSelectionResponse selectedFinalMenu
) {
    public static VoteCurrentStateResponse from(VoteCurrentState state) {
        return new VoteCurrentStateResponse(
                state.sessionStatus(),
                state.candidates().stream().map(MenuCandidateResponse::from).toList(),
                state.menuVote().orElse(null),
                state.finalMenuVote().orElse(null),
                state.selectedFinalMenu().map(FinalMenuSelectionResponse::from).orElse(null)
        );
    }
}
