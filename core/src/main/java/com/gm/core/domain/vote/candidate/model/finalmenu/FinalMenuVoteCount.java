package com.gm.core.domain.vote.candidate.model.finalmenu;

import java.util.UUID;

/** 최종투표 후보별 현재 집계다. */
public record FinalMenuVoteCount(UUID candidateId, int count) {
}
