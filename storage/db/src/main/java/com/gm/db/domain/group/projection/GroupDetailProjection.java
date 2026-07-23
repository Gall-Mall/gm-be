package com.gm.db.domain.group.projection;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import com.gm.core.domain.group.model.GroupMemberRole;

/**
 * 그룹 상세 조회 쿼리의 결과를 담는 프로젝션이다.
 *
 * <p>{@code GroupEntity}를 감싸지 않고 필요한 필드를 그대로 펼쳐, 조회 결과가
 * 영속성 컨텍스트에 attach된 엔티티를 들고 있지 않도록 한다.</p>
 */
public record GroupDetailProjection(
        UUID id,
        UUID ownerUserId,
        String name,
        String locationAddress,
        Double latitude,
        Double longitude,
        int searchRadiusM,
        LocalTime recommendationTime,
        int maxMemberCount,
        long memberCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        GroupMemberRole currentUserRole
) {
}
