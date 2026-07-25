package com.gm.api.controller.vote.candidate.dto.request;

import jakarta.validation.constraints.NotNull;

import com.gm.core.domain.vote.candidate.model.MenuVoteChoice;

/** 메뉴 후보 투표 요청. */
public record MenuVoteRequest(
        @NotNull MenuVoteChoice choice
) {
}
