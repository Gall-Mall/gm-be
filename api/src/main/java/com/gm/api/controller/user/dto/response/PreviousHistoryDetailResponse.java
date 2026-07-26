package com.gm.api.controller.user.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.gm.core.domain.history.model.PreviousHistoryDetail;
import com.gm.core.domain.history.model.PreviousVoteSessionHistory;

public record PreviousHistoryDetailResponse(
        UUID groupId,
        String groupName,
        UUID voteSessionId,
        String name,
        String url,
        String address,
        Double latitude,
        Double longitude,
        Integer distanceM,
        String externalPlaceId,
        int goCount,
        int maybeCount,
        int noCount,
        LocalDateTime completedAt
) {

    public static PreviousHistoryDetailResponse from(PreviousHistoryDetail detail) {
        PreviousVoteSessionHistory voteSession = detail.voteSession();
        return new PreviousHistoryDetailResponse(
                detail.groupId(),
                detail.groupName(),
                voteSession.voteSessionId(),
                voteSession.name(),
                voteSession.url(),
                voteSession.address(),
                voteSession.latitude(),
                voteSession.longitude(),
                voteSession.distanceM(),
                voteSession.externalPlaceId(),
                voteSession.goCount(),
                voteSession.maybeCount(),
                voteSession.noCount(),
                voteSession.completedAt()
        );
    }
}
