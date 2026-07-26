package com.gm.core.domain.vote.event;

import java.util.List;

import com.gm.core.domain.vote.candidate.model.MenuVoteResult;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;

/** 1차 투표 마감 후 DB에 저장된 집계·판정과 다음 세션 상태다. */
public record MenuVoteClosedData(
        List<MenuVoteResult> results,
        VoteSessionStatus sessionStatus
) {
    public MenuVoteClosedData {
        results = List.copyOf(results);
    }
}
