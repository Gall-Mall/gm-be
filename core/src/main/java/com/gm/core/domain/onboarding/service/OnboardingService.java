package com.gm.core.domain.onboarding.service;

import com.gm.core.domain.onboarding.model.Onboarding;
import com.gm.core.domain.user.service.UserService;
import com.gm.core.domain.user_setting.service.UserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserService userService;
    private final UserSettingService userSettingService;

    /**
     *  온보딩 제출
     */
    @Transactional
    public void submitOnboarding(UUID userId, Onboarding onboarding) {
//        userService.updateTermsAgreed(userId, onboarding.termsAgreed());
        userSettingService.changeUserSetting(userId, onboarding.userSetting());
    }
}
