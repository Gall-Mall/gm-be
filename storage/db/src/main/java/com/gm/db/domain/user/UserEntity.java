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

    /** 비표준 알레르기 자유텍스트. AI 하드 제외 지시용. (schema: user.custom_allergen_text) */
    @Column(name = "custom_allergen_text", length = 500)
    private String customAllergenText;

    /** 선호 음식 자유텍스트. AI 소프트 신호. (schema: user.preference_text) */
    @Column(name = "preference_text", length = 500)
    private String preferenceText;

    /** 싫어하는 음식 자유텍스트. AI 소프트 신호. (schema: user.exclude_food_text) */
    @Column(name = "exclude_food_text", length = 500)
    private String excludeFoodText;

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