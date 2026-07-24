package com.gm.core.domain.recommendation.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gm.core.domain.recommendation.model.CuratedCandidate;
import com.gm.core.domain.recommendation.model.CuratedMenu;
import com.gm.core.domain.recommendation.model.MenuCurationCommand;
import com.gm.core.domain.recommendation.port.MenuCurationPort;

import lombok.RequiredArgsConstructor;

/**
 * 결정론 후보를 AI로 큐레이션하고, 결과를 화이트리스트로 검증해 저장 가능한 후보로 만드는 서비스.
 *
 * AI가 후보 목록 밖 이름이나 중복을 반환해도 여기서 걸러진다. 반환 이름은 후보 목록에 있는
 * 것만 통과하며, 순서(추천 순위)는 AI가 준 순서를 보존한다.
 */
@Service
@RequiredArgsConstructor
public class RecommendationCurationService {

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
        // 이름 → id 역인덱스. 동일 이름이 여러 id를 가질 경우 첫 번째만 사용한다.
        Map<String, UUID> idByNormalizedName = new LinkedHashMap<>();
        candidatesByMenuId.forEach((id, name) ->
                idByNormalizedName.putIfAbsent(normalize(name), id));

        List<CuratedMenu> curated = menuCurationPort.curate(new MenuCurationCommand(
                candidatesByMenuId.values().stream().toList(),
                nullSafe(allergenExclusionTexts),
                nullSafe(preferenceTexts),
                nullSafe(excludeFoodTexts),
                maxCount
        ));

        if (curated == null) {
            return List.of();
        }

        // 후보 목록에 있는 이름만 통과. AI 순서(추천 순위) 보존, id 중복 제거, 상한 적용.
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

    private List<String> nullSafe(List<String> texts) {
        return texts == null ? List.of() : texts.stream().filter(t -> t != null && !t.isBlank()).toList();
    }

    private String normalize(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
