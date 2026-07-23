package com.gm.db.domain.group.entity;

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

    /**
     * 초대를 통해 가입하는 일반 멤버를 생성한다.
     */
    public static GroupMemberEntity ofMember(UUID diningGroupId, UUID userId) {
        return new GroupMemberEntity(
                diningGroupId,
                userId,
                GroupMemberStatus.ACTIVE,
                LocalDateTime.now(),
                GroupMemberRole.MEMBER
        );
    }

    /**
     * 자발적으로 탈퇴(LEFT)했던 멤버십 행을 일반 멤버로 재활성화한다. 유니크 제약
     * ({@code UK_group_member})이 그룹당 회원 한 명에 행 하나만 허용하므로, 재가입은 새 행을
     * 삽입하지 않고 기존 행을 갱신하는 방식으로 처리한다.
     */
    public void rejoin() {
        this.status = GroupMemberStatus.ACTIVE;
        this.joinedAt = LocalDateTime.now();
        this.leftAt = null;
        this.role = GroupMemberRole.MEMBER;
    }
}
