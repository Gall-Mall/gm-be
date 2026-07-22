package com.gm.db.domain.user;

import com.gm.core.domain.user.model.Provider;
import com.gm.core.domain.user.model.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.gm.core.domain.user.model.User;
import com.gm.db.common.entity.BaseEntity;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {

    @Column(length = 30, nullable = false)
    private String name;
    @Column(length = 100, nullable = false)
    private String nickname;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;
    @Column(length = 255, nullable = false)
    private String providerId;
    @Column(length = 20, nullable = false)
    private String phone;
    @Column(length = 255, nullable = false)
    private String email;
    @Column(nullable = false)
    private Boolean termsAgreed;

    public UserEntity(
            String name,
            String nickname,
            UserStatus status,
            Provider provider,
            String providerId,
            String phone,
            String email,
            Boolean termsAgreed) {
        this.name = name;
        this.nickname = nickname;
        this.status = status;
        this.provider = provider;
        this.providerId = providerId;
        this.phone = phone;
        this.email = email;
        this.termsAgreed = termsAgreed;
    }
}