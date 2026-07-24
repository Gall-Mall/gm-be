package com.gm.client.openai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.gm.core.domain.user.model.ExtractedFoodPreference;

/**
 * AI가 JSON mode로 반환하는 음식 취향 추출 결과의 역직렬화 전용 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FoodPreferenceExtractionJson(
        @JsonProperty("foods") List<String> foods
) {

    /** null 필드를 빈 값으로 보정해 도메인 모델로 변환한다. */
    public ExtractedFoodPreference toDomain() {
        return new ExtractedFoodPreference(foods == null ? List.of() : foods);
    }
}
