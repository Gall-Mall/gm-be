package com.gm.api.controller.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.gm.core.domain.user.model.FoodPreferencePolarity;

/**
 * 자유텍스트 음식 취향 분석 요청.
 *
 * <p>좋아하는/싫어하는 입력칸이 같은 엔드포인트를 쓰므로, 어느 칸에서 온 요청인지를
 * polarity로 함께 받는다. 이 값이 없으면 "면요리는 싫고 국물은 좋아요"처럼 한 문장에
 * 양극성이 섞였을 때 두 키워드가 모두 한쪽으로 저장된다.</p>
 */
public record FoodPreferenceAnalyzeRequest(
        @NotBlank(message = "분석할 텍스트를 입력해주세요.")
        @Size(max = 1000, message = "텍스트는 1000자 이하여야 합니다.")
        String text,

        @NotNull(message = "분석할 취향의 극성(LIKE/EXCLUDE)을 지정해주세요.")
        FoodPreferencePolarity polarity
) {
}
