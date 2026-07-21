package com.gm.db.domain.group;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.model.NewGroup;
import com.gm.db.common.entity.BaseEntity;

@Entity
@Table(name = "`group`")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupEntity extends BaseEntity {

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "location_address", nullable = false, length = 500)
    private String locationAddress;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "search_radius_m", nullable = false)
    private int searchRadiusM;

    @Column(name = "recommendation_time", nullable = false)
    private LocalTime recommendationTime;

    @Column(name = "max_member_count", nullable = false)
    private int maxMemberCount;

    public GroupEntity(NewGroup newGroup) {
        this.ownerUserId = newGroup.ownerUserId();
        this.name = newGroup.name();
        this.locationAddress = newGroup.locationAddress();
        this.latitude = newGroup.latitude();
        this.longitude = newGroup.longitude();
        this.searchRadiusM = newGroup.searchRadiusM();
        this.recommendationTime = newGroup.recommendationTime();
        this.maxMemberCount = newGroup.maxMemberCount();
    }

    public Group toDomainModel(int memberCount) {
        return new Group(
                getId(),
                ownerUserId,
                name,
                locationAddress,
                latitude,
                longitude,
                searchRadiusM,
                recommendationTime,
                maxMemberCount,
                memberCount,
                getCreatedAt(),
                getUpdatedAt()
        );
    }
}
