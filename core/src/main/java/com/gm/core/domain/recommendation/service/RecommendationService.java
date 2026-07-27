package com.gm.core.domain.recommendation.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gm.core.domain.recommendation.model.MemberPreference;
import com.gm.core.domain.recommendation.model.MenuInfo;
import com.gm.core.domain.recommendation.model.Recency;
import com.gm.core.domain.recommendation.model.ScoredMenu;
import com.gm.core.domain.recommendation.repository.RecommendationRepository;

import lombok.RequiredArgsConstructor;

/**
 * 그룹 메뉴 후보를 결정론적으로 필터링·점수화·정렬하는 서비스.
 * 스코어링은 순수 계산 로직이며, 알레르기 제외도 여기서 결정론적으로 처리한다(LLM 위임 X).
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationRepository repository;

    private static final double MENU_LIKE = 1.0;      // 메뉴 선호
    private static final double CAT_LIKE = 0.4;       // 카테고리 선호
    private static final double CAT_DISLIKE = - 0.4;  // 카테고리 불호
    private static final double R_SEL = 2.0;          // 최근 먹음 감점 강도
    private static final int W_DAYS = 14;             // 최근성 감쇠 윈도우
    private static final double BETA = 0.2;           // 인기 가중
    private static final int PREFERRED_MAX_PER_CATEGORY = 3;


    /**
     * 그룹의 추천 데이터를 조회해 상위 {@code topN}개 메뉴를 점수순으로 반환한다.
     */
    public List<ScoredMenu> recommend(UUID groupId, Set<UUID> shown, int topN) {
        List<MemberPreference> members = repository.findMemberPreferencesByGroupId(groupId);
        List<MenuInfo> menus = repository.findAllMenus();
        Map<UUID, Recency> recency = repository.findRecencyByGroupId(groupId);
        Map<UUID, Double> popularity = repository.findMenuPopularity();
        return scoreAndRank(members, menus, recency, popularity, shown, topN);
    }

    /**
     * 후보 메뉴를 필터링·점수화해 상위 {@code topN}개를 내림차순으로 반환한다.
     * recommend()의 내부 단계이며, 순수 계산이라 단위 테스트를 위해 package-private로 연다.
     */
    List<ScoredMenu> scoreAndRank(
            List<MemberPreference> members,
            List<MenuInfo> menus,
            Map<UUID, Recency> recency,
            Map<UUID, Double> popularity,
            Set<UUID> shown,
            int topN
    ){
        List<ScoredMenu> ranked = menus.stream()
                .filter(m -> isAllowed(m, members, shown))
                .map(m -> new ScoredMenu(m.menuId(), score(m, members, recency, popularity)))
                .sorted(Comparator.comparingDouble(ScoredMenu::score).reversed())
                .toList();
        Map<UUID, UUID> categoryByMenuId = new HashMap<>();
        menus.forEach(menu -> categoryByMenuId.put(menu.menuId(), menu.categoryId()));
        return diversifyCategories(ranked, categoryByMenuId, topN);
    }

    /**
     * 점수순을 유지하면서 한 카테고리가 후보 풀을 독점하지 않도록 분산한다.
     *
     * <p>카테고리당 상한을 {@code PREFERRED_MAX_PER_CATEGORY}부터 시작해 목표 수를 채울 때까지
     * 1씩 올리며 반복 순회한다. 각 순회 안에서는 점수 내림차순을 그대로 지키므로 우선순위는
     * 보존되고, 상한이 매 순회 한 칸씩만 열리므로 특정 카테고리가 남은 자리를 통째로 가져가지
     * 못한다.</p>
     *
     * <p>예전에는 2차 순회에서 상한 없이 점수순으로 남은 자리를 채웠다. 그 결과 점수가 동점인
     * 신규 그룹에서는 1차의 분산이 무의미해지고, 메뉴가 많은 카테고리가 후보의 절반을
     * 차지했다.</p>
     */
    private List<ScoredMenu> diversifyCategories(
            List<ScoredMenu> ranked,
            Map<UUID, UUID> categoryByMenuId,
            int topN
    ) {
        if (topN <= 0 || ranked.isEmpty()) {
            return List.of();
        }
        int target = Math.min(topN, ranked.size());
        List<ScoredMenu> selected = new ArrayList<>(target);
        Set<UUID> selectedMenuIds = new HashSet<>();
        Map<UUID, Integer> categoryCounts = new HashMap<>();

        for (int cap = PREFERRED_MAX_PER_CATEGORY; selected.size() < target; cap++) {
            for (ScoredMenu menu : ranked) {
                if (selected.size() >= target) {
                    break;
                }
                if (selectedMenuIds.contains(menu.menuId())) {
                    continue;
                }
                UUID categoryId = categoryByMenuId.get(menu.menuId());
                if (categoryCounts.getOrDefault(categoryId, 0) >= cap) {
                    continue;
                }
                selected.add(menu);
                selectedMenuIds.add(menu.menuId());
                categoryCounts.merge(categoryId, 1, Integer::sum);
            }
        }
        return List.copyOf(selected);
    }

    /**
     * 하드 필터. 한 명이라도 배제 사유가 있으면 후보에서 뺀다.
     * ex. 알레르기, 싫어하는 메뉴
     */
    private boolean isAllowed(MenuInfo m, List<MemberPreference> members, Set<UUID> shown){
        if(shown.contains(m.menuId())){ // 재시도 중복 시 false
            return false;
        }
        for(MemberPreference mp : members){
            if(!Collections.disjoint(m.allergenIds(), mp.standardAllergens())){ // 알레르기의 경우 제외
                return false;
            }
            if(mp.excludeMenus().contains(m.menuId())){ // 메뉴 EXCLUDE인 경우 제외
                return false;
            }
        }
        return true;
    }

    /**
     * 최종 점수 = 선호 점수 − 최근성 감점 + 인기 가중.
     */
    private double score(MenuInfo m, List<MemberPreference> members,
                         Map<UUID, Recency> recency, Map<UUID, Double> popularity){

        return preferenceScore(m, members) - recencyPenalty(recency.get(m.menuId()))
                + BETA * popularity.getOrDefault(m.menuId(), 0.0);
    }

    /**
     * 멤버별 선호 점수의 평균.
     */
    private double preferenceScore(MenuInfo m, List<MemberPreference> members){
        return members.stream().mapToDouble(mp -> {
            double s = 0.0;
            if(mp.likedMenus().contains(m.menuId())){
                s += MENU_LIKE;
            }
            else if(mp.likedCategories().contains(m.categoryId())){
                s += CAT_LIKE;
            }
            else if(mp.dislikedCategories().contains(m.categoryId())){
                s += CAT_DISLIKE;
            }
            return s;
        }).average().orElse(0.0);
    }

    /**
     * 최근 먹은 메뉴 감점. 먹은 적 없으면 0, 최근일수록 큰 감점.
     */
    private double recencyPenalty(Recency r){
        if(r == null || r.daysSinceSelected() == null){
            return 0.0;
        }
        return R_SEL * decay(r.daysSinceSelected());
    }

    /**
     * 선형 감소함수 형태
     * 0일이면 1.0, {@code W_DAYS}일 이후엔 0.
     */
    private double decay(long days){
        return Math.max(0.0, 1.0 - (double) days / W_DAYS);
    }
}
