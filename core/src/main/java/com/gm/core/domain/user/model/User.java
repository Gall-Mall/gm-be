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
        String preferenceText,
        String excludeFoodText
) {
    /**
     * OAuth 로그인을 통해 신규 사용자를 생성한다.
     * 신규 사용자는 온보딩과 약관 동의를 완료하지 않은 상태이므로 ONBOARDING 상태와 termsAgreed=false로 생성한다.
     *
     * @param name 사용자 이름
     * @param provider OAuth 제공자
     * @param providerId OAuth 제공자의 사용자 식별자
     * @param phone 휴대폰 번호
     * @param email 이메일
     * @return 신규 사용자
     */

    public static User create(
            String name,
            Provider provider,
            String providerId,
            String phone,
            String email
    ) {
        return new User(
                name,
                name,
                UserStatus.ONBOARDING,
                provider,
                providerId,
                phone,
                email,
                false,
                null,
                null,
                null

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
                this.preferenceText,
                this.excludeFoodText

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
                preferenceText,
                this.excludeFoodText
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
                this.termsAgreed,
                customAllergenText,
                this.preferenceText,
                this.excludeFoodText
        );
    }

    public User updateExcludeFoodText(String excludeFoodText) {
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
                this.preferenceText,
                excludeFoodText
        );
    }
}