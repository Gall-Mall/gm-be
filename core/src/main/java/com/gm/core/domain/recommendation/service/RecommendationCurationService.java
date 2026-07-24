package com.gm.core.domain.recommendation.service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gm.core.domain.recommendation.model.CuratedCandidate;
import com.gm.core.domain.recommendation.model.CuratedMenu;
import com.gm.core.domain.recommendation.model.MenuCurationCommand;
import com.gm.core.domain.recommendation.port.MenuCurationPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결정론 후보를 AI로 큐레이션하고, 결과를 화이트리스트로 검증해 저장 가능한 후보로 만드는 서비스.
 *
 * AI가 후보 목록 밖 이름이나 중복을 반환해도 여기서 걸러진다. 반환 이름은 후보 목록에 있는
 * 것만 통과하며, 순서(추천 순위)는 AI가 준 순서를 보존한다.
 *
 * 안전: 표준 알레르기는 결정론 스코어링이 이미 후보에서 제외한다. 성분 매핑이 없는 비표준
 * 알레르기(custom_allergen_text)는 이름 기반 결정론 필터로 후보 풀에서 사전 제외하여,
 * AI(soft 방어)를 유일한 방어선으로 두지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationCurationService {

    private static final int MIN_ALLERGEN_TOKEN_LENGTH = 2;   // 1글자 토큰의 오탐 방지

    private final MenuCurationPort menuCurationPort;

    /**
     * 후보 풀(menuId→이름)을 AI로 큐레이션해 검증된 최종 후보를 순서대로 반환한다.
     * allergenExclusionTexts는 하드 제외, preferenceTexts·excludeFoodTexts는 소프트 신호다.
     */
    public List<CuratedCandidate> curate(
            Map<UUID, String> candidatesByMenuId,
            List<String> allergenExclusionTexts,
            List<String> preferenceTexts,
            List<String> excludeFoodTexts,
            int maxCount
    ) {
        // 비표준 알레르기 토큰이 이름에 든 후보를 사전 제외한다. AI에 애초에 노출하지 않으므로
        // AI가 지시를 놓치거나 인젝션돼도 알레르기 유발 메뉴가 후보로 돌아올 수 없다.
        Set<String> allergenTokens = tokenize(allergenExclusionTexts);
        Map<UUID, String> safePool = new LinkedHashMap<>();
        candidatesByMenuId.forEach((id, name) -> {
            if (containsAllergen(name, allergenTokens)) {
                log.info("[curation] 비표준 알레르기 후보 제외: {}", name);
            } else {
                safePool.put(id, name);
            }
        });

        // 이름 → id 역인덱스. 동일 이름이 여러 id를 가질 경우 첫 번째만 사용한다.
        Map<String, UUID> idByNormalizedName = new LinkedHashMap<>();
        safePool.forEach((id, name) -> {
            String key = normalize(name);
            if (idByNormalizedName.putIfAbsent(key, id) != null) {
                log.debug("[curation] 동명 메뉴 중복으로 일부 id 누락 가능: {}", name);
            }
        });

        List<CuratedMenu> curated = menuCurationPort.curate(new MenuCurationCommand(
                safePool.values().stream().toList(),
                nullSafe(allergenExclusionTexts),
                nullSafe(preferenceTexts),
                nullSafe(excludeFoodTexts),
                maxCount
        ));

        if (curated == null) {
            return List.of();
        }

        // 안전 후보 목록에 있는 이름만 통과. AI 순서(추천 순위) 보존, id 중복 제거, 상한 적용.
        LinkedHashMap<UUID, CuratedCandidate> result = new LinkedHashMap<>();
        for (CuratedMenu menu : curated) {
            if (menu == null || menu.menuName() == null) {
                continue;
            }
            UUID menuId = idByNormalizedName.get(normalize(menu.menuName()));
            if (menuId == null || result.containsKey(menuId)) {
                continue;
            }
            result.put(menuId, new CuratedCandidate(menuId, menu.description()));
            if (result.size() >= maxCount) {
                break;
            }
        }
        return List.copyOf(result.values());
    }

    /** 알레르기 자유텍스트를 콤마·공백으로 쪼개 정규화 토큰 집합으로 만든다. */
    private Set<String> tokenize(List<String> texts) {
        if (texts == null) {
            return Set.of();
        }
        Set<String> tokens = new HashSet<>();
        for (String text : texts) {
            if (text == null) {
                continue;
            }
            for (String part : text.split("[,\\s]+")) {
                String token = normalize(part);
                if (token.length() >= MIN_ALLERGEN_TOKEN_LENGTH) {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    /** 메뉴 이름에 알레르기 토큰이 부분 문자열로 들어있는지 검사한다. */
    private boolean containsAllergen(String menuName, Set<String> allergenTokens) {
        if (allergenTokens.isEmpty()) {
            return false;
        }
        String normalized = normalize(menuName);
        for (String token : allergenTokens) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private List<String> nullSafe(List<String> texts) {
        return texts == null ? List.of() : texts.stream().filter(t -> t != null && !t.isBlank()).toList();
    }

    private String normalize(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
