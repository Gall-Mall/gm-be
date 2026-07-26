package com.gm.core.domain.vote.candidate.model;

import java.util.List;
import java.util.Optional;

import com.gm.core.domain.vote.candidate.model.FinalMenuVoteState;
import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.model.MenuVoteState;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;

/** 화면 진입·Socket 재연결 시 한 번에 동기화할 투표 세션 현재 상태다. */
public record VoteCurrentState(
        VoteSessionStatus sessionStatus,
        List<MenuVoteCandidate> candidates,
        Optional<MenuVoteState> menuVote,
        Optional<FinalMenuVoteState> finalMenuVote,
        Optional<VoteCandidate> selectedFinalMenu
) {
    public VoteCurrentState {
        candidates = List.copyOf(candidates);
        menuVote = menuVote == null ? Optional.empty() : menuVote;
        finalMenuVote = finalMenuVote == null ? Optional.empty() : finalMenuVote;
        selectedFinalMenu = selectedFinalMenu == null ? Optional.empty() : selectedFinalMenu;
    }
}
