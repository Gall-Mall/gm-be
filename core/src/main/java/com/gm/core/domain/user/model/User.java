package com.gm.core.domain.user.model;

public record User(
        String name,
        String nickname,
        UserStatus status,
        Provider provider,
        String providerId,
        String phone,
        String email,
        Boolean termsAgreed,
        String customAllergenText,
        String preferenceText
) {
    private static final String DEFAULT_STATUS = "ACTIVE";

    public static User create(
            String name,
            UserStatus status,
            Provider provider,
            String providerId,
            String phone,
            String email,
            Boolean termsAgreed,
            String customAllergenText,
            String preferenceText
    ) {
        return new User(
                name,
                name,
                status,
                provider,
                providerId,
                phone,
                email,
                false,
                "",
                ""

        );
    }
    public User updateTermsAgreed(Boolean termsAgreed) {
        return new User(
                this.name,
                this.nickname,
                this.status,
                this.provider,
                this.providerId,
                this.phone,
                this.email,
                termsAgreed,
                this.customAllergenText,
                this.preferenceText()
        );
    }
    public User updatePreferenceText(String preferenceText) {
        return new User(
                this.name,
                this.nickname,
                this.status,
                this.provider,
                this.providerId,
                this.phone,
                this.email,
                this.termsAgreed,
                this.customAllergenText,
                preferenceText
        );
    }

    public User updateCustomAllergenText(String customAllergenText) {
        return new User(
                this.name,
                this.nickname,
                this.status,
                this.provider,
                this.providerId,
                this.phone,
                this.email,
                termsAgreed,
                customAllergenText,
                this.preferenceText
        );
    }
}