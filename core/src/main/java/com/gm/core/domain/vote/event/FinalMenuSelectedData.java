package com.gm.core.domain.vote.event;

import java.util.UUID;

import com.gm.core.domain.vote.session.model.VoteSessionStatus;

/** 최종 메뉴 확정 이벤트의 DB 반영 결과다. */
public record FinalMenuSelectedData(
        UUID selectedCandidateId,
        UUID selectedMenuId,
        VoteSessionStatus sessionStatus
) {
}
