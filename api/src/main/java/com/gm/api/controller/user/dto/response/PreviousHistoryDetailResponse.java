package com.gm.api.controller.user.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.gm.core.domain.history.model.PreviousHistoryRecord;

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
        LocalDateTime createdAt
) {

    public static PreviousHistoryDetailResponse from(PreviousHistoryRecord record) {
        return new PreviousHistoryDetailResponse(
                record.groupId(),
                record.groupName(),
                record.voteSessionId(),
                record.restaurantName(),
                record.url(),
                record.address(),
                record.latitude(),
                record.longitude(),
                record.distanceM(),
                record.externalPlaceId(),
                record.goCount(),
                record.maybeCount(),
                record.noCount(),
                record.restaurantCreatedAt()
        );
    }
}
