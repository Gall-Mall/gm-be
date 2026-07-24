package com.gm.core.domain.user.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.gm.core.domain.menu.model.Allergen;
import com.gm.core.domain.menu.repository.AllergenRepository;
import com.gm.core.domain.user.model.AllergenExtractionResult;
import com.gm.core.domain.user.model.ExtractedAllergen;
import com.gm.core.domain.user.port.AiChatPort;

import lombok.RequiredArgsConstructor;

/**
 * 자유텍스트 알레르기 추출 서비스 (동기).
 *
 * 흐름: 마스터 조회 → [사용자입력 + 마스터 이름 목록] AI 위임 → 화이트리스트 검증 →
 * 이름→UUID 매핑. AI가 무엇을 뽑았든 판정 기준은 오직 마스터다.
 * 마스터에 있으면 표준(id 확정), 없으면 비표준(텍스트 보관)이라 무엇도 소실되지 않는다.
 */
@Service
@RequiredArgsConstructor
public class AllergenExtractionService {

    // 모델 출력을 신뢰하지 않고 상한을 둔다. (프롬프트 인젝션·비정상 응답 방어)
    private static final int MAX_NAME_LENGTH = 30;     // 개별 이름 최대 길이
    // 비표준은 custom_allergen_text VARCHAR(500)에 콤마 조인 저장된다.
    // 개수 × (길이 + 구분자) 가 500 이내가 되도록 상한을 잡는다. (15 × 32 = 480)
    private static final int MAX_CUSTOM_COUNT = 15;    // 비표준 알레르기 최대 개수

    private final AiChatPort aiChatPort;
    private final AllergenRepository allergenRepository;

    /**
     * 자유텍스트에서 알레르기를 추출하고 표준 알레르기를 마스터와 매칭한다.
     */
    public AllergenExtractionResult extract(String freeText) {
        List<Allergen> master = allergenRepository.findAll();

        ExtractedAllergen raw = aiChatPort.extractAllergens(
                freeText,
                master.stream().map(Allergen::name).toList()
        );

        Map<String, Allergen> byNormalizedName = new LinkedHashMap<>();
        for (Allergen allergen : master) {
            byNormalizedName.putIfAbsent(normalize(allergen.name()), allergen);
        }

        // null/위험문자/공백/길이 정제 (콤마·HTML 문자 제거로 저장 계약·XSS 방어). 음식 서비스와 동일 정책.
        List<String> names = PreferenceKeywords.cleaned(raw.allergenNames(), MAX_NAME_LENGTH);

        // 표준 = 마스터에 정확히 매칭되는 것만. 매칭은 개수 제한하지 않는다. (user_allergen에 id 저장)
        List<Allergen> matched = names.stream()
                .map(name -> byNormalizedName.get(normalize(name)))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 비표준 = 마스터 미매칭 나머지. 개수 상한은 비표준에만 적용한다. (custom_allergen_text 보관)
        List<String> custom = names.stream()
                .filter(name -> !byNormalizedName.containsKey(normalize(name)))
                .limit(MAX_CUSTOM_COUNT)
                .toList();

        return new AllergenExtractionResult(matched, custom);
    }

    /** 공백·대소문자 차이로 매칭이 어긋나지 않도록 이름을 정규화한다. */
    private String normalize(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);
    }
}
