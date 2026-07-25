package com.gm.api.controller.user.dto.response;

import com.gm.core.domain.user.model.UserStatus;

/**
 * 온보딩 완료 여부와 완료 후 회원 상태를 반환한다.
 */
public record OnboardingSubmitResponse(
        boolean onboardingCompleted,
        UserStatus status
) {

    public static OnboardingSubmitResponse completed() {
        return new OnboardingSubmitResponse(true, UserStatus.ACTIVE);
    }
}
