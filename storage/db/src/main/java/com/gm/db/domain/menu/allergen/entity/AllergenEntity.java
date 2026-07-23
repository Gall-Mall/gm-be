package com.gm.db.domain.menu.allergen.entity;

import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "allergen")
@Getter
public class AllergenEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;
}
