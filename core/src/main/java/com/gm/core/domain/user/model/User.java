package com.gm.core.domain.user.model;

public record User(
        String name,
        String nickname,
        UserStatus status,
        Provider provider,
        String providerId,
        String phone,
        String email,
        Boolean termsAgreed
) {
    private static final String DEFAULT_STATUS = "ACTIVE";

    public static User create(
            String name,
            UserStatus status,
            Provider provider,
            String providerId,
            String phone,
            String email
    ) {
        return new User(
                name,
                name,
                status,
                provider,
                providerId,
                phone,
                email,
                false
        );
    }
}