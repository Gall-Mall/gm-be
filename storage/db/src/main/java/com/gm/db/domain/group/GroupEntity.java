package com.gm.db.domain.group;

import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.gm.db.common.entity.BaseEntity;

@Entity
@Table(name = "dining_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupEntity extends BaseEntity {

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "location_address", nullable = false, length = 500)
    private String locationAddress;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "search_radius_m", nullable = false)
    private int searchRadiusM;

    @Column(name = "recommendation_time", nullable = false)
    private LocalTime recommendationTime;

    @Column(name = "max_member_count", nullable = false)
    private int maxMemberCount;

    public GroupEntity(
            UUID ownerUserId,
            String name,
            String locationAddress,
            Double latitude,
            Double longitude,
            int searchRadiusM,
            LocalTime recommendationTime,
            int maxMemberCount
    ) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.locationAddress = locationAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.searchRadiusM = searchRadiusM;
        this.recommendationTime = recommendationTime;
        this.maxMemberCount = maxMemberCount;
    }
}
