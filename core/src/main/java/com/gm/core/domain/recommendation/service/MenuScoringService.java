package com.gm.core.domain.recommendation.service;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gm.core.domain.recommendation.model.MemberPreference;
import com.gm.core.domain.recommendation.model.MenuInfo;
import com.gm.core.domain.recommendation.model.Recency;
import com.gm.core.domain.recommendation.model.ScoredMenu;

@Slf4j
@Service
public class MenuScoringService {

    private static final double MENU_LIKE = 1.0;      // 메뉴 선호
    private static final double CAT_LIKE = 0.4;       // 카테고리 선호
    private static final double CAT_DISLIKE = - 0.4;  // 카테고리 불호
    private static final double R_SEL = 2.0;          // 최근 먹음 감점 강도
    private static final int W_DAYS = 14;             // 최근성 감쇠 윈도우
    private static final double BETA = 0.2;           // 인기 가중


    /**
     *
     * @param members
     * @param menus
     * @param recency
     * @param popularity
     * @param shown
     * @param topN
     * @return
     */
    public List<ScoredMenu> scoreAndRank(
            List<MemberPreference> members,
            List<MenuInfo> menus,
            Map<UUID, Recency> recency,
            Map<UUID, Double> popularity,
            Set<UUID> shown,
            int topN
    ){
        return menus.stream()
                .filter(m -> isAllowed(m, members, shown))
                .map(m -> new ScoredMenu(m.menuId(), score(m, members, recency, popularity)))
                .sorted(Comparator.comparingDouble(ScoredMenu::score).reversed())
                .limit(topN)
                .toList();
    }

    /**
     *
     * @param m
     * @param members
     * @param shown
     * @return
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
     *
     * @param m
     * @param members
     * @return
     */
    private double score(MenuInfo m, List<MemberPreference> members,
                         Map<UUID, Recency> recency, Map<UUID, Double> popularity){

        return preferenceScore(m, members) - recencyPenalty(recency.get(m.menuId()))
                + BETA * popularity.getOrDefault(m.menuId(), 0.0);
    }

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

    private double recencyPenalty(Recency r){
        if(r == null || r.daysSinceSelected() == null){
            return 0.0;
        }
        return R_SEL * decay(r.daysSinceSelected());
    }

    private double decay(long days){
        return Math.max(0.0, 1.0 - (double) days / W_DAYS);
    }
}
