package com.gm.api.controller.user;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.user.dto.request.PreferenceAnalyzeRequest;
import com.gm.api.controller.user.dto.response.PreferenceAnalyzeResponse;
import com.gm.api.controller.user.dto.response.UserResponse;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.user.service.PreferenceExtractionService;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final PreferenceExtractionService preferenceExtractionService;

    @GetMapping("/me")
    public ResponseEnvelope<UserResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEnvelope.success(UserResponse.from(principal.getUser()));
    }

    /**
     * 자유텍스트에서 알레르기·호불호를 동기로 추출한다.
     * 사용자가 결과를 즉시 확인해야 하므로 MQ를 타지 않는다. 저장은 온보딩 제출에서 별도로 한다.
     */
    @PostMapping("/me/preferences/analyze")
    public ResponseEnvelope<PreferenceAnalyzeResponse> analyzePreference(
            @Valid @RequestBody PreferenceAnalyzeRequest request
    ) {
        return ResponseEnvelope.success(
                PreferenceAnalyzeResponse.from(preferenceExtractionService.extract(request.text()))
        );
    }
}
