package com.gm.api.controller.vote.candidate.dto.response;

import java.util.List;

import com.gm.core.domain.vote.candidate.model.finalmenu.FinalMenuVoteState;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteState;
import com.gm.core.domain.vote.candidate.model.state.VoteCurrentState;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;

/** 화면 진입 또는 Socket 재연결 직후 동기화할 투표 세션 기준 상태다. */
public record VoteCurrentStateResponse(
        VoteSessionStatus sessionStatus,
        List<MenuCandidateResponse> candidates,
        MenuVoteState menuVote,
        FinalMenuVoteState finalMenuVote,
        FinalMenuSelectionResponse selectedFinalMenu
) {
    /** 투표 세션의 현재 상태를 재동기화 API 응답으로 변환한다. */
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
