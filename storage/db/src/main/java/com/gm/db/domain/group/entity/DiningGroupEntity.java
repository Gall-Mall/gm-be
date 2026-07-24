package com.gm.db.domain.group.entity;

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
public class DiningGroupEntity extends BaseEntity {

    /**
     * 그룹을 최초로 생성한 회원이다(표시 전용).
     *
     * <p><b>주의:</b> 방장 권한 판단의 근거가 아니다. 현재 방장 여부는 반드시
     * {@code group_member}의 역할(role)·상태(status)를 기준으로 판단해야 한다. 향후
     * 방장 위임 기능이 추가되면 이 값과 실제 방장이 달라질 수 있다.</p>
     */
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

    public DiningGroupEntity(
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

    public void update(
            String name,
            String locationAddress,
            Double latitude,
            Double longitude,
            int searchRadiusM,
            LocalTime recommendationTime,
            int maxMemberCount
    ) {
        this.name = name;
        this.locationAddress = locationAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.searchRadiusM = searchRadiusM;
        this.recommendationTime = recommendationTime;
        this.maxMemberCount = maxMemberCount;
    }
}
