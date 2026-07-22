package com.gm.db.domain.user_preference.user_allergen;

import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_allergen")
@NoArgsConstructor
public class UserAllergenEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID allergenId;

    public UserAllergenEntity(UUID userId, UUID allergenId) {
        this.userId = userId;
        this.allergenId = allergenId;
    }
}
