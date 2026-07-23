package com.gm.client.openai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.gm.core.domain.user.model.ExtractedPreference;

/**
 * AI가 JSON mode로 반환하는 추출 결과의 역직렬화 전용 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PreferenceExtractionJson(
        @JsonProperty("standard_allergens") List<String> standardAllergens,
        @JsonProperty("custom_allergens") List<String> customAllergens,
        @JsonProperty("preference_text") String preferenceText,
        @JsonProperty("exclude_food_text") String excludeFoodText
) {

    /** null 필드를 빈 값으로 보정해 도메인 모델로 변환한다. */
    public ExtractedPreference toDomain() {
        return new ExtractedPreference(
                standardAllergens == null ? List.of() : standardAllergens,
                customAllergens == null ? List.of() : customAllergens,
                preferenceText == null ? "" : preferenceText,
                excludeFoodText == null ? "" : excludeFoodText
        );
    }
}
