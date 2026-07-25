package com.gm.api.controller.user;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.user.dto.request.AllergenAnalyzeRequest;
import com.gm.api.controller.user.dto.request.FoodPreferenceAnalyzeRequest;
import com.gm.api.controller.user.dto.request.OnboardingSubmitRequest;
import com.gm.api.controller.user.dto.request.UserSettingRequest;
import com.gm.api.controller.user.dto.response.AllergenAnalyzeResponse;
import com.gm.api.controller.user.dto.response.FoodPreferenceAnalyzeResponse;
import com.gm.api.controller.user.dto.response.OnboardingSubmitResponse;
import com.gm.api.controller.user.dto.response.UserResponse;
import com.gm.api.controller.user.dto.response.UserSettingResponse;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.model.UserSetting;
import com.gm.core.domain.user.service.AllergenExtractionService;
import com.gm.core.domain.user.service.FoodPreferenceExtractionService;
import com.gm.core.domain.user.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final AllergenExtractionService allergenExtractionService;
    private final FoodPreferenceExtractionService foodPreferenceExtractionService;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEnvelope<UserResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEnvelope.success(UserResponse.from(principal.getUser()));
    }

    /**
     * 자유텍스트에서 알레르기를 동기로 추출한다.
     * 사용자가 결과를 즉시 확인해야 하므로 MQ를 타지 않는다. 저장은 온보딩 제출에서 별도로 한다.
     */
    @PostMapping("/me/allergens/analyze")
    public ResponseEnvelope<AllergenAnalyzeResponse> analyzeAllergen(
            @Valid @RequestBody AllergenAnalyzeRequest request
    ) {
        return ResponseEnvelope.success(
                AllergenAnalyzeResponse.from(allergenExtractionService.extract(request.text()))
        );
    }

    /**
     * 자유텍스트에서 음식 취향을 동기로 추출한다. (카테고리 매칭 + 잔여 텍스트)
     * 좋아하는/싫어하는 입력칸 공용이며, 극성은 온보딩 제출에서 확정한다.
     */
    @PostMapping("/me/food-preferences/analyze")
    public ResponseEnvelope<FoodPreferenceAnalyzeResponse> analyzeFoodPreference(
            @Valid @RequestBody FoodPreferenceAnalyzeRequest request
    ) {
        return ResponseEnvelope.success(
                FoodPreferenceAnalyzeResponse.from(foodPreferenceExtractionService.extract(request.text()))
        );
    }

    /**
     * 약관 동의와 최초 회원 설정을 제출해 온보딩을 완료한다.
    */
    @PostMapping("/me/onboarding")
    public ResponseEnvelope<OnboardingSubmitResponse> submitOnboarding(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody OnboardingSubmitRequest request
    ) {
        userService.submitOnboarding(principal.getUserId(), request.toDomain());
        return ResponseEnvelope.success(OnboardingSubmitResponse.completed());
    }

    /**
     * 현재 인증 회원의 설정을 조회한다.
     */
    @GetMapping("/me/food-settings")
    public ResponseEnvelope<UserSettingResponse> getUserSetting(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEnvelope.success(
                UserSettingResponse.from(userService.getUserSetting(principal.getUserId()))
        );
    }

    /**
     * 현재 인증 회원의 설정 전체를 요청 값으로 교체한다.
     */
    @PutMapping("/me/food-settings")
    public ResponseEnvelope<UserSettingResponse> changeUserSetting(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody UserSettingRequest request
    ) {
        UserSetting userSetting = request.toDomain();
        userService.changeUserSetting(principal.getUserId(), userSetting);
        return ResponseEnvelope.success(UserSettingResponse.from(userSetting));
    }
}
