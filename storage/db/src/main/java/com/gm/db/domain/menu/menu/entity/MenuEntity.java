package com.gm.db.domain.menu.menu.entity;

import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "menu")
@Getter
public class MenuEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private String name;

    private String imageUrl;
}
