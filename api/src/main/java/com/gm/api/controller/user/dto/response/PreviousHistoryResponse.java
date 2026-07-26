package com.gm.api.controller.user.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.gm.core.domain.history.model.PreviousGroupHistory;
import com.gm.core.domain.history.model.PreviousVoteSessionHistory;

public record PreviousHistoryResponse(
        List<PreviousGroupResponse> previous
) {

    public static PreviousHistoryResponse from(List<PreviousGroupHistory> history) {
        return new PreviousHistoryResponse(
                history.stream()
                        .map(PreviousGroupResponse::from)
                        .toList()
        );
    }

    public record PreviousGroupResponse(
            UUID groupId,
            String name,
            List<PreviousVoteSessionResponse> voteSessions
    ) {

        private static PreviousGroupResponse from(PreviousGroupHistory history) {
            return new PreviousGroupResponse(
                    history.groupId(),
                    history.name(),
                    history.voteSessions().stream()
                            .map(PreviousVoteSessionResponse::from)
                            .toList()
            );
        }
    }

    public record PreviousVoteSessionResponse(
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

        private static PreviousVoteSessionResponse from(PreviousVoteSessionHistory history) {
            return new PreviousVoteSessionResponse(
                    history.voteSessionId(),
                    history.name(),
                    history.url(),
                    history.address(),
                    history.latitude(),
                    history.longitude(),
                    history.distanceM(),
                    history.externalPlaceId(),
                    history.goCount(),
                    history.maybeCount(),
                    history.noCount(),
                    history.completedAt()
            );
        }
    }
}
