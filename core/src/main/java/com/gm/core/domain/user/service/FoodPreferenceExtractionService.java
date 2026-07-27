package com.gm.core.domain.user.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.gm.core.domain.menu.model.Category;
import com.gm.core.domain.menu.model.Menu;
import com.gm.core.domain.menu.repository.CategoryRepository;
import com.gm.core.domain.menu.repository.MenuRepository;
import com.gm.core.domain.user.model.ExtractedFoodPreference;
import com.gm.core.domain.user.model.FoodPreferenceExtractionResult;
import com.gm.core.domain.user.model.FoodPreferencePolarity;
import com.gm.core.domain.user.port.FoodPreferenceAiPort;

import lombok.RequiredArgsConstructor;

/**
 * 자유텍스트 음식 취향 추출 서비스 (동기).
 *
 * 흐름: 메뉴 마스터 조회 → [사용자입력 + 메뉴 이름 목록] AI 위임 → 화이트리스트 검증 →
 * 이름→UUID 매핑. 사용자가 말한 구체적 메뉴(파스타·칼국수)를 그대로 뽑고, 마스터에 매칭되면
 * 메뉴(id 확정)로 확정한다. 메뉴는 상위 카테고리로 일반화하지 않는다.
 *
 * <p>메뉴에 매칭되지 않은 키워드는 카테고리 마스터("한식", "중식" …)와 한 번 더 대조한다.
 * 카테고리로 확정되면 user_category_preference에 id로 저장되어 추천 스코어링의 카테고리
 * 가중치가 실제로 적용된다. 여기서도 안 걸리는 것만 자유텍스트로 보존한다.</p>
 */
@Service
@RequiredArgsConstructor
public class FoodPreferenceExtractionService {

    // 모델 출력을 신뢰하지 않고 상한을 둔다. (프롬프트 인젝션·비정상 응답 방어)
    private static final int MAX_NAME_LENGTH = 30;        // 개별 이름 최대 길이
    // 잔여 텍스트는 preference_text/exclude_food_text VARCHAR(500)에 콤마 조인 저장된다.
    // 개수 × (길이 + 구분자) 가 500 이내가 되도록 상한을 잡고, 최종 문자열도 하드 캡한다.
    private static final int MAX_UNMATCHED_COUNT = 15;    // 비매칭(텍스트) 최대 개수
    private static final int MAX_TEXT_LENGTH = 500;       // 저장 컬럼 길이

    private final FoodPreferenceAiPort foodPreferenceAiPort;
    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 자유텍스트에서 음식 취향을 추출하고 메뉴·카테고리를 마스터와 매칭한다.
     *
     * @param polarity 어느 입력칸에서 온 요청인지. 한 문장에 좋아함·싫어함이 섞여도
     *                 이 극성에 해당하는 키워드만 추출된다.
     */
    public FoodPreferenceExtractionResult extract(String freeText, FoodPreferencePolarity polarity) {
        List<Menu> master = menuRepository.findAll();

        ExtractedFoodPreference raw = foodPreferenceAiPort.extractFoodPreference(
                freeText,
                master.stream().map(Menu::name).toList(),
                polarity
        );

        Map<String, Menu> byNormalizedName = new LinkedHashMap<>();
        for (Menu menu : master) {
            byNormalizedName.putIfAbsent(normalize(menu.name()), menu);
        }

        // null/위험문자/공백/길이 정제 (콤마·HTML 문자 제거로 저장 계약·XSS 방어). 알레르기 서비스와 동일 정책.
        List<String> keywords = PreferenceKeywords.cleaned(raw.foodKeywords(), MAX_NAME_LENGTH);

        // 메뉴 = 마스터에 정확히 매칭되는 것만. 매칭은 개수 제한하지 않는다. (user_menu_preference에 id 저장)
        List<Menu> matched = keywords.stream()
                .map(name -> byNormalizedName.get(normalize(name)))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 메뉴에 없는 키워드를 카테고리 마스터와 대조한다. "한식"처럼 카테고리 단위로 말한 취향을
        // 텍스트로 흘려보내면 추천 스코어링의 카테고리 가중치가 영영 적용되지 않는다.
        Map<String, Category> categoryByNormalizedName = new LinkedHashMap<>();
        for (Category category : categoryRepository.findAll()) {
            categoryByNormalizedName.putIfAbsent(normalize(category.name()), category);
        }

        List<Category> matchedCategories = keywords.stream()
                .filter(name -> !byNormalizedName.containsKey(normalize(name)))
                .map(name -> categoryByNormalizedName.get(normalize(name)))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 나머지 = 메뉴·카테고리 모두 미매칭 → 텍스트로 보존. 개수 상한은 비매칭에만 적용한다.
        // 각 항목은 이미 콤마가 제거됐으므로 조인해도 경계가 안전하다. 최종 길이도 컬럼 상한으로 캡한다.
        List<String> unmatched = keywords.stream()
                .filter(name -> !byNormalizedName.containsKey(normalize(name)))
                .filter(name -> !categoryByNormalizedName.containsKey(normalize(name)))
                .limit(MAX_UNMATCHED_COUNT)
                .toList();
        String unmatchedText = String.join(", ", unmatched);
        if (unmatchedText.length() > MAX_TEXT_LENGTH) {
            unmatchedText = unmatchedText.substring(0, MAX_TEXT_LENGTH);
        }

        return new FoodPreferenceExtractionResult(matched, matchedCategories, unmatchedText);
    }

    /** 공백·대소문자 차이로 매칭이 어긋나지 않도록 이름을 정규화한다. */
    private String normalize(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
