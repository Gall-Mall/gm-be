package com.gm.api.controller.user;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.user.dto.request.AllergenAnalyzeRequest;
import com.gm.api.controller.user.dto.request.FoodPreferenceAnalyzeRequest;
import com.gm.api.controller.user.dto.response.AllergenAnalyzeResponse;
import com.gm.api.controller.user.dto.response.FoodPreferenceAnalyzeResponse;
import com.gm.api.controller.user.dto.response.UserResponse;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.service.AllergenExtractionService;
import com.gm.core.domain.user.service.FoodPreferenceExtractionService;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final AllergenExtractionService allergenExtractionService;
    private final FoodPreferenceExtractionService foodPreferenceExtractionService;

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
}
