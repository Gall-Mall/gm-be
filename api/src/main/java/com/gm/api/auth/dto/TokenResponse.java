package com.gm.api.auth.dto;

import com.gm.core.domain.user.model.UserStatus;

public record TokenResponse(String tokenType, String accessToken, UserStatus userStatus, String redirectPath) {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String ONBOARDING_REDIRECT_PATH = "/onboarding";
    private static final String ACTIVE_REDIRECT_PATH = "/";

    /**
     * 발급된 Access Token과 사용자 상태를 로그인 성공 응답으로 변환한다.
     *
     * @param accessToken 발급된 Access Token
     * @param userStatus 현재 사용자 상태
     * @return 사용자 상태에 따른 이동 경로가 포함된 토큰 응답
     */
    public static TokenResponse of(String accessToken, UserStatus userStatus) {
        return new TokenResponse(TOKEN_TYPE, accessToken, userStatus, resolveRedirectPath(userStatus));
    }

    /**
     * 사용자 상태에 따른 프런트엔드 이동 경로를 결정한다.
     * ONBOARDING 사용자는 온보딩 화면으로 이동하고, ACTIVE 사용자는 서비스 메인 화면으로 이동한다.
     * WITHDRAWN 사용자는 정상적인 로그인 성공 응답을 받을 수 없다.
     *
     * @param userStatus 현재 사용자 상태
     * @return 상태에 대응하는 이동 경로
     */
    private static String resolveRedirectPath(UserStatus userStatus) {
        return switch (userStatus) {
            case ONBOARDING -> ONBOARDING_REDIRECT_PATH;
            case ACTIVE -> ACTIVE_REDIRECT_PATH;
            case WITHDRAWN ->
                    throw new IllegalStateException("탈퇴한 사용자는 로그인 성공 응답을 생성할 수 없습니다.");
        };
    }
}