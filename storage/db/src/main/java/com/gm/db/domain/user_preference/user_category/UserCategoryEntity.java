package com.gm.db.domain.user_preference.user_category;

import com.gm.db.common.entity.BaseEntity;
import com.gm.core.domain.user_preference.user_category.CategoryPreference;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_category_preference")
@NoArgsConstructor
public class UserCategoryEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryPreference preference;

    public UserCategoryEntity(UUID userId, UUID categoryId, CategoryPreference preference) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.preference = preference;
    }
}
