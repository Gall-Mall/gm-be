package com.gm.db.domain.group;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.model.GroupMemberStatus;
import com.gm.db.common.entity.BaseEntity;

@Entity
@Table(
        name = "group_member",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_group_member",
                columnNames = {"dining_group_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMemberEntity extends BaseEntity {

    @Column(name = "dining_group_id", nullable = false)
    private UUID diningGroupId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GroupMemberStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 6)
    private GroupMemberRole role;

    private GroupMemberEntity(
            UUID diningGroupId,
            UUID userId,
            GroupMemberStatus status,
            LocalDateTime joinedAt,
            GroupMemberRole role
    ) {
        this.diningGroupId = diningGroupId;
        this.userId = userId;
        this.status = status;
        this.joinedAt = joinedAt;
        this.role = role;
    }

    /**
     * 그룹 생성 시점의 그룹장 멤버를 생성한다.
     */
    public static GroupMemberEntity ofOwner(UUID diningGroupId, UUID userId) {
        return new GroupMemberEntity(
                diningGroupId,
                userId,
                GroupMemberStatus.ACTIVE,
                LocalDateTime.now(),
                GroupMemberRole.OWNER
        );
    }
}
