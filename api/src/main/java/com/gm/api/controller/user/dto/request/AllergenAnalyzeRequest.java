package com.gm.api.controller.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 자유텍스트 알레르기 분석 요청.
 *
 * @param text 사용자가 입력한 자유텍스트 (알레르기 서술)
 */
public record AllergenAnalyzeRequest(
        @NotBlank(message = "분석할 텍스트를 입력해주세요.")
        @Size(max = 1000, message = "텍스트는 1000자 이하여야 합니다.")
        String text
) {
}
