package com.gm.core.domain.user.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.gm.core.domain.menu.model.Category;
import com.gm.core.domain.menu.repository.CategoryRepository;
import com.gm.core.domain.user.model.ExtractedFoodPreference;
import com.gm.core.domain.user.model.FoodPreferenceExtractionResult;
import com.gm.core.domain.user.port.FoodPreferenceAiPort;

import lombok.RequiredArgsConstructor;

/**
 * 자유텍스트 음식 취향 추출 서비스 (동기).
 *
 * <p>흐름: 카테고리 마스터 조회 → [사용자입력 + 카테고리 이름 목록] AI 위임 → 화이트리스트 검증 →
 * 이름→UUID 매핑. 마스터에 매칭되면 카테고리(id 확정), 아니면 취향 텍스트로 보존한다.
 * 메뉴는 여기서 다루지 않는다(아코디언/보드 선택).</p>
 */
@Service
@RequiredArgsConstructor
public class FoodPreferenceExtractionService {

    // 모델 출력을 신뢰하지 않고 상한을 둔다. (프롬프트 인젝션·비정상 응답 방어)
    private static final int MAX_KEYWORD_COUNT = 20;   // 취향 키워드 최대 개수
    private static final int MAX_NAME_LENGTH = 30;     // 개별 이름 최대 길이

    private final FoodPreferenceAiPort foodPreferenceAiPort;
    private final CategoryRepository categoryRepository;

    /**
     * 자유텍스트에서 음식 취향을 추출하고 카테고리를 마스터와 매칭한다.
     */
    public FoodPreferenceExtractionResult extract(String freeText) {
        List<Category> master = categoryRepository.findAll();

        ExtractedFoodPreference raw = foodPreferenceAiPort.extractFoodPreference(
                freeText,
                master.stream().map(Category::name).toList()
        );

        Map<String, Category> byNormalizedName = new LinkedHashMap<>();
        for (Category category : master) {
            byNormalizedName.putIfAbsent(normalize(category.name()), category);
        }

        // null/공백 제거 + 개별 길이 상한 + 개수 상한으로 정제한다.
        List<String> keywords = (raw.foodKeywords() == null ? List.<String>of() : raw.foodKeywords()).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty() && name.length() <= MAX_NAME_LENGTH)
                .distinct()
                .limit(MAX_KEYWORD_COUNT)
                .toList();

        // 카테고리 = 마스터에 정확히 매칭되는 것만. (user_category_preference에 id로 저장)
        List<Category> matched = keywords.stream()
                .map(name -> byNormalizedName.get(normalize(name)))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 나머지 = 카테고리로 분류되지 않은 취향 → 텍스트로 보존. (preference_text/exclude_food_text)
        String unmatchedText = String.join(", ", keywords.stream()
                .filter(name -> !byNormalizedName.containsKey(normalize(name)))
                .toList());

        return new FoodPreferenceExtractionResult(matched, unmatchedText);
    }

    /** 공백·대소문자 차이로 매칭이 어긋나지 않도록 이름을 정규화한다. */
    private String normalize(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
