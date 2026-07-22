package com.gm.db.domain.user_preference.user_category;

import com.gm.db.common.entity.BaseEntity;
import com.gm.core.domain.user_setting.user_preference.user_menu.model.UserPreference;
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
    private UserPreference preference;

    public UserCategoryEntity(UUID userId, UUID categoryId, UserPreference preference) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.preference = preference;
    }
}
