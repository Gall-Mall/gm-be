package com.gm.core.domain.history.model;

import java.util.List;
import java.util.UUID;

/**
 * 한 식사 그룹의 지난 투표 결과 목록이다.
 */
public record PreviousGroupHistory(
        UUID groupId,
        String name,
        List<PreviousVoteSessionHistory> voteSessions
) {

    public PreviousGroupHistory {
        voteSessions = List.copyOf(voteSessions);
    }
}
