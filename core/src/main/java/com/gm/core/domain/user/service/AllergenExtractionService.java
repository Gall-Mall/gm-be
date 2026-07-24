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
 * <p>흐름: 마스터 조회 → [사용자입력 + 마스터 이름 목록] AI 위임 → 화이트리스트 검증 →
 * 이름→UUID 매핑. AI가 무엇을 뽑았든 판정 기준은 오직 마스터다.
 * 마스터에 있으면 표준(id 확정), 없으면 비표준(텍스트 보관)이라 무엇도 소실되지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class AllergenExtractionService {

    // 모델 출력을 신뢰하지 않고 상한을 둔다. (프롬프트 인젝션·비정상 응답 방어)
    private static final int MAX_CUSTOM_COUNT = 20;    // 비표준 알레르기 최대 개수
    private static final int MAX_NAME_LENGTH = 30;     // 개별 이름 최대 길이

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

        // null/공백 원소를 제거하고 개별 길이 상한을 적용해 정제한다. (모델 출력 무검증 방지)
        List<String> names = (raw.allergenNames() == null ? List.<String>of() : raw.allergenNames()).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty() && name.length() <= MAX_NAME_LENGTH)
                .toList();

        // 표준 = 마스터에 정확히 매칭되는 것만. (user_allergen에 id로 저장)
        List<Allergen> matched = names.stream()
                .map(name -> byNormalizedName.get(normalize(name)))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 비표준 = 마스터에 매칭되지 않는 나머지. 개수 상한으로 저장 폭주를 막는다.
        // (user.custom_allergen_text VARCHAR(500)로 보관)
        List<String> custom = names.stream()
                .filter(name -> !byNormalizedName.containsKey(normalize(name)))
                .distinct()
                .limit(MAX_CUSTOM_COUNT)
                .toList();

        return new AllergenExtractionResult(matched, custom);
    }

    /** 공백·대소문자 차이로 매칭이 어긋나지 않도록 이름을 정규화한다. */
    private String normalize(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);
    }
}
