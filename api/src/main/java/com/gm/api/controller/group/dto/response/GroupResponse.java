package com.gm.api.controller.group.dto.response;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gm.core.domain.group.model.Group;

public record GroupResponse(
        UUID groupId,

        /**
         * 그룹을 최초로 생성한 회원 식별자(표시 전용).
         * 방장 권한 판단 근거가 아니다 — 현재 방장 여부는 {@code group_member}의 역할·상태
         * 기준으로 판단한다.
         */
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
