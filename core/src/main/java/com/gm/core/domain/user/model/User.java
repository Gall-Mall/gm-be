package com.gm.core.domain.user.model;

public record User(
        String name,
        String nickname,
        String status,
        String provider,
        String providerId,
        String phone,
        String email,
        Boolean termsAgreed
) {
}