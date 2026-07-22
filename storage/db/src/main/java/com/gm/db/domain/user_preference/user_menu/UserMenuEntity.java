package com.gm.db.domain.user_preference.user_menu;

import com.gm.core.domain.user_preference.user_menu.UserPreference;
import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_menu_preference")
@NoArgsConstructor
public class UserMenuEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID menuId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserPreference preference;

    public UserMenuEntity(UUID userId, UUID menuId, UserPreference preference) {
        this.userId = userId;
        this.menuId = menuId;
        this.preference = preference;
    }
}
