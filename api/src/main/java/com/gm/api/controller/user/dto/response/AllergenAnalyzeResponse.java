package com.gm.api.controller.user.dto.response;

import java.util.List;
import java.util.UUID;

import com.gm.core.domain.user.model.AllergenExtractionResult;

/**
 * 자유텍스트 알레르기 분석 결과. 사용자가 확인·수정한 뒤 온보딩 제출에서 확정 저장한다.
 * FE는 standardAllergens로 표준 체크박스를 켜고, customAllergens는 칩으로 표시한다.
 */
public record AllergenAnalyzeResponse(
        List<AllergenResponse> standardAllergens,
        List<String> customAllergens
) {

    public record AllergenResponse(UUID id, String name) {
    }

    public static AllergenAnalyzeResponse from(AllergenExtractionResult result) {
        return new AllergenAnalyzeResponse(
                result.standardAllergens().stream()
                        .map(a -> new AllergenResponse(a.id(), a.name()))
                        .toList(),
                result.customAllergens()
        );
    }
}
