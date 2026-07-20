package com.gm.db.domain.store;

import com.gm.core.domain.store.model.Store;
import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommended_restaurant")
@AllArgsConstructor
@NoArgsConstructor
public class StoreEntity extends BaseEntity {

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

    public static StoreEntity from(Store store) {
        return new StoreEntity(
                false,
                store.placeName(),
                store.placeUrl(),
                store.roadAddress(),
                store.coordinate().y(),
                store.coordinate().y(),
                Integer.parseInt(store.distance()),
                store.placeId()
        );
    }
}
