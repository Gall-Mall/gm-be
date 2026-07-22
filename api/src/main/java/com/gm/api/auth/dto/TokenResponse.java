package com.gm.api.auth.dto;

public record TokenResponse(String tokenType, String accessToken) {

    private static final String TOKEN_TYPE = "Bearer";

    public static TokenResponse of(String accessToken) {
        return new TokenResponse(TOKEN_TYPE, accessToken);
    }
}