package com.gm.api.controller.user.dto.response;

import com.gm.core.domain.user.model.User;

public record UserResponse(
        String name,
        String nickname,
        String status,
        String provider,
        String providerId,
        String phone,
        String email,
        Boolean termsAgreed
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.name(),
                user.nickname(),
                user.status(),
                user.provider(),
                user.providerId(),
                user.phone(),
                user.email(),
                user.termsAgreed()
        );
    }
}