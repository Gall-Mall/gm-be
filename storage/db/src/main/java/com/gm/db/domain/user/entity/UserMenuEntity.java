package com.gm.db.domain.user.entity;

import com.gm.core.domain.user.model.UserMenuPreference;
import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.*;
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
    private UserMenuPreference preference;

    public UserMenuEntity(UUID userId, UUID menuId, UserMenuPreference preference) {
        this.userId = userId;
        this.menuId = menuId;
        this.preference = preference;
    }
}
