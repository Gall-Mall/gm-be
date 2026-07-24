package com.gm.api.controller.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 자유텍스트 음식 취향 분석 요청. 좋아하는/싫어하는 입력칸 공용(극성은 저장 시점 확정).
 */
public record FoodPreferenceAnalyzeRequest(
        @NotBlank(message = "분석할 텍스트를 입력해주세요.")
        @Size(max = 1000, message = "텍스트는 1000자 이하여야 합니다.")
        String text
) {
}
