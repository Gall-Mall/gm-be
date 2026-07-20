package com.gm.db.domain.store;

import com.gm.core.domain.store.model.Store;
import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Entity
@Table(name = "recommended_restaurant")
public class StoreEntity extends BaseEntity {

    @Id
    @Column(name ="id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID recommendedRestaurantId;

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
    private BigDecimal latitude;

    @Column(nullable = false)
    private BigDecimal longitude;

    @Column(name = "distance_m", nullable = false)
    private Integer distance;

    @Column(name = "external_place_id", nullable = false)
    private String externalPlaceId;

    public static StoreEntity from(Store store) {
        StoreEntity storeEntity = new StoreEntity();
//      storeEntity.voteSession
        storeEntity.selected = false;
        storeEntity.name = store.placeName();
        storeEntity.url = store.placeUrl();
        storeEntity.address = store.roadAddress();
        storeEntity.latitude = BigDecimal.valueOf(store.coordinate().y());
        storeEntity.longitude = BigDecimal.valueOf(store.coordinate().x());
        storeEntity.distance = Integer.parseInt(store.distance());
        storeEntity.externalPlaceId = store.placeId();
        return storeEntity;

    }
}
