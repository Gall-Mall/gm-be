package com.gm.db.domain.food_master.allergen;

import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "allergen")
public class AllergenEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;
}
