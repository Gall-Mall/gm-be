package com.gm.api.controller.group.dto.response;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gm.core.domain.group.model.Group;

public record GroupResponse(
        UUID groupId,
        UUID ownerUserId,
        String name,
        String locationAddress,
        Double latitude,
        Double longitude,
        int searchRadiusM,
        @JsonFormat(pattern = "HH:mm")
        LocalTime recommendationTime,
        int maxMemberCount,
        int memberCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GroupResponse from(Group group) {
        return new GroupResponse(
                group.id(),
                group.ownerUserId(),
                group.name(),
                group.locationAddress(),
                group.latitude(),
                group.longitude(),
                group.searchRadiusM(),
                group.recommendationTime(),
                group.maxMemberCount(),
                group.memberCount(),
                group.createdAt(),
                group.updatedAt()
        );
    }
}
