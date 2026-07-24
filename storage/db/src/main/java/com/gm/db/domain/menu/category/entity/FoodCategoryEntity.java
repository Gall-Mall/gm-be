package com.gm.db.domain.menu.category.entity;

import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "food_category")
@Getter
public class FoodCategoryEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;
}
