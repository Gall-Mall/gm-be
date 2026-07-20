package com.gm.db.domain.store;

import com.gm.core.domain.store.model.Provider;
import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommended_restaurant")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class StoreEntity extends BaseEntity {

    public StoreEntity(Boolean selected, String name, String url, String address, Double latitude, Double longitude, Integer distance, String externalPlaceId, Provider provider) {
        this.selected = selected;
        this.name = name;
        this.url = url;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
        this.externalPlaceId = externalPlaceId;
        this.provider = provider;
    }

    // TODO: 연관관계 매핑 UUID voteSession;

    @Column(name = "selected", nullable = false)
    private Boolean selected;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "distance_m", nullable = false)
    private Integer distance;

    @Column(name = "external_place_id", nullable = false)
    private String externalPlaceId;

    @Enumerated(EnumType.STRING)
    private Provider provider;
}
