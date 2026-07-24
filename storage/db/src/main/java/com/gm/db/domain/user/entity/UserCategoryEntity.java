package com.gm.db.domain.user.entity;

import com.gm.core.domain.user.model.UserCategoryPreference;
import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "user_category_preference")
@NoArgsConstructor
@Getter
public class UserCategoryEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserCategoryPreference preference;

    public UserCategoryEntity(UUID userId, UUID categoryId, UserCategoryPreference preference) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.preference = preference;
    }
}
