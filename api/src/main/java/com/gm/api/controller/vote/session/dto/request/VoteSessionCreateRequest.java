package com.gm.api.controller.vote.session.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 수동 투표 세션 생성 요청이다.
 *
 * @param title 세션 제목
 * @param likeKeyword 종합 선호 키워드
 * @param dislikeKeyword 종합 비선호 키워드
 */
public record VoteSessionCreateRequest(
        @NotBlank
        @Size(max = 150)
        String title,

        @Size(max = 255)
        String likeKeyword,

        @Size(max = 255)
        String dislikeKeyword
) {
}