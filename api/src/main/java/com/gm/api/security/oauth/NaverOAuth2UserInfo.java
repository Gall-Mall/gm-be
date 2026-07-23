package com.gm.api.security.oauth;

import java.util.Map;

public class NaverOAuth2UserInfo {

    private final Map<String, Object> response;

    @SuppressWarnings("unchecked")
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {

        Object response = attributes.get("response");

        if (!(response instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("네이버 사용자 정보(response)가 존재하지 않습니다.");
        }

        this.response = (Map<String, Object>) map;
    }

    public String providerId() {
        String providerId = (String) response.get("id");

        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("네이버 사용자 식별자가 존재하지 않습니다.");
        }

        return providerId;
    }
    public String name() { return (String) response.get("name"); }
    public String email() { return (String) response.get("email"); }
    public String phone() { return (String) response.get("mobile"); }
}