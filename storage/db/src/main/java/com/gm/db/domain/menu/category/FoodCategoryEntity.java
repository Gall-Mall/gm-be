package com.gm.db.domain.menu.category;

import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "food_category")
public class FoodCategoryEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;
}
