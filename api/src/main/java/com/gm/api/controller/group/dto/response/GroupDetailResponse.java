package com.gm.api.controller.group.dto.response;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gm.core.domain.group.model.GroupDetail;
import com.gm.core.domain.group.model.GroupMemberRole;

public record GroupDetailResponse(
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
        LocalDateTime updatedAt,
        GroupMemberRole currentUserRole
) {
    public static GroupDetailResponse from(GroupDetail groupDetail) {
        return new GroupDetailResponse(
                groupDetail.group().id(),
                groupDetail.group().ownerUserId(),
                groupDetail.group().name(),
                groupDetail.group().locationAddress(),
                groupDetail.group().latitude(),
                groupDetail.group().longitude(),
                groupDetail.group().searchRadiusM(),
                groupDetail.group().recommendationTime(),
                groupDetail.group().maxMemberCount(),
                groupDetail.group().memberCount(),
                groupDetail.group().createdAt(),
                groupDetail.group().updatedAt(),
                groupDetail.currentUserRole()
        );
    }
}
