package com.gm.db.domain.food_master.menu;

import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "menu")
public class MenuEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private String name;

    private String imageUrl;
}
