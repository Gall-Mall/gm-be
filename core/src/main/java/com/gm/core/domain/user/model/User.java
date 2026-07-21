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
    private static final String DEFAULT_STATUS = "ACTIVE";

    public static User create(
            String name,
            String provider,
            String providerId,
            String phone,
            String email
    ) {
        return new User(
                name,
                name,
                DEFAULT_STATUS,
                provider,
                providerId,
                phone,
                email,
                false
        );
    }
}