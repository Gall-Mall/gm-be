package com.gm.db.domain.user;

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

    private String name;
    private String nickname;
    private String status;
    private String provider;
    private String providerId;
    private String phone;
    private String email;
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