package com.gm.db.domain.user;

import jakarta.persistence.Column;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.gm.core.domain.user.model.User;
import com.gm.db.common.entity.BaseEntity;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String nickname;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private String provider;
    @Column(nullable = false)
    private String providerId;
    @Column(nullable = false)
    private String phone;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private Boolean termsAgreed;

    public UserEntity(
            String name,
            String nickname,
            String status,
            String provider,
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