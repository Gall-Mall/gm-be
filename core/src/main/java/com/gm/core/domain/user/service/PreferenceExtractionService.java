package com.gm.core.domain.user.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.gm.core.domain.menu.model.Allergen;
import com.gm.core.domain.menu.repository.AllergenRepository;
import com.gm.core.domain.user.port.AiChatPort;
import com.gm.core.domain.user.model.ExtractedPreference;
import com.gm.core.domain.user.model.PreferenceExtractionResult;

import lombok.RequiredArgsConstructor;

/**
 * 자유텍스트 선호 추출 서비스 (동기).
 *
 * <p>흐름: 마스터 조회 → [사용자입력 + 마스터 이름 목록] AI 위임 → 화이트리스트 검증 →
 * 이름→UUID 매핑. AI가 목록 밖 이름을 반환해도 여기서 걸러지므로 DB에는 검증된 id만 저장된다.</p>
 */
@Service
@RequiredArgsConstructor
public class PreferenceExtractionService {

    private final AiChatPort aiChatPort;
    private final AllergenRepository allergenRepository;

    /**
     * 자유텍스트에서 알레르기·호불호를 추출하고 표준 알레르기를 마스터와 매칭한다.
     */
    public PreferenceExtractionResult extract(String freeText) {
        List<Allergen> master = allergenRepository.findAll();

        ExtractedPreference raw = aiChatPort.extractPreference(
                freeText,
                master.stream().map(Allergen::name).toList()
        );

        Map<String, Allergen> byNormalizedName = new LinkedHashMap<>();
        for (Allergen allergen : master) {
            byNormalizedName.putIfAbsent(normalize(allergen.name()), allergen);
        }

        // 화이트리스트 검증: AI가 표준을 비표준으로 잘못 분류했을 수 있으므로
        // 두 목록을 합쳐 마스터에 있는 이름만 표준으로 확정한다. (중복 제거 포함)
        List<Allergen> matched = Stream
                .concat(raw.standardAllergens().stream(), raw.customAllergens().stream())
                .map(name -> byNormalizedName.get(normalize(name)))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 비표준 알레르기 = 마스터에 없는 이름들만. (user.custom_allergen_text로 보관될 값)
        List<String> custom = raw.customAllergens().stream()
                .filter(name -> !byNormalizedName.containsKey(normalize(name)))
                .distinct()
                .toList();

        return new PreferenceExtractionResult(
                matched,
                custom,
                raw.preferenceText(),
                raw.excludeFoodText()
        );
    }

    /** 공백·대소문자 차이로 매칭이 어긋나지 않도록 이름을 정규화한다. */
    private String normalize(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "").toLowerCase();
    }
}
