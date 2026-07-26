package com.gm.core.domain.history.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 완료된 투표 세션의 최종 선택 식당 기록이다.
 */
public record PreviousVoteSessionHistory(
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

    public static PreviousVoteSessionHistory from(PreviousHistoryRecord record) {
        return new PreviousVoteSessionHistory(
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
