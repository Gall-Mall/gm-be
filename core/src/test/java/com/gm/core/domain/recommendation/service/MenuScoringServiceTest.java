package com.gm.core.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.gm.core.domain.recommendation.model.MemberPreference;
import com.gm.core.domain.recommendation.model.MenuInfo;
import com.gm.core.domain.recommendation.model.Recency;
import com.gm.core.domain.recommendation.model.ScoredMenu;

class MenuScoringServiceTest {

    private final MenuScoringService service = new MenuScoringService();

    // 필드 순서: userId, likedMenus, likedCategories, excludeMenus, dislikedCategories, standardAllergens
    private MemberPreference member(Set<UUID> likedMenus, Set<UUID> likedCats,
                                    Set<UUID> excludeMenus, Set<UUID> dislikedCats,
                                    Set<UUID> allergens) {
        return new MemberPreference(UUID.randomUUID(),
                likedMenus, likedCats, excludeMenus, dislikedCats, allergens);
    }

    private MenuInfo menu(UUID id, UUID cat, Set<UUID> allergens) {
        return new MenuInfo(id, cat, allergens);
    }

    private List<UUID> rankedIds(List<ScoredMenu> ranked) {
        return ranked.stream().map(ScoredMenu::menuId).toList();
    }

    @Test
    void 선호_우선순위대로_정렬된다() {
        UUID 제육 = UUID.randomUUID(), 김치찌개 = UUID.randomUUID(),
             초밥 = UUID.randomUUID(), 짜장면 = UUID.randomUUID();
        UUID 한식 = UUID.randomUUID(), 일식 = UUID.randomUUID(), 중식 = UUID.randomUUID();

        // 제육 콕 좋아함 + 한식 좋아함 + 중식 싫어함
        var member = member(Set.of(제육), Set.of(한식), Set.of(), Set.of(중식), Set.of());

        var menus = List.of(
                menu(제육, 한식, Set.of()),      // 메뉴 LIKE(override) →  1.0
                menu(김치찌개, 한식, Set.of()),   // 카테고리 LIKE       →  0.4
                menu(초밥, 일식, Set.of()),       // 무관               →  0.0
                menu(짜장면, 중식, Set.of())      // 카테고리 DISLIKE    → -0.4
        );

        var ranked = service.scoreAndRank(List.of(member), menus, Map.of(), Map.of(), Set.of(), 10);

        assertThat(rankedIds(ranked)).containsExactly(제육, 김치찌개, 초밥, 짜장면);
    }

    @Test
    void 더_많은_멤버가_좋아할수록_상위에_온다() {
        UUID 김치찌개 = UUID.randomUUID(), 초밥 = UUID.randomUUID();
        UUID 한식 = UUID.randomUUID(), 일식 = UUID.randomUUID();

        // A: 한식·일식 둘 다 좋아 / B: 한식만 좋아
        var a = member(Set.of(), Set.of(한식, 일식), Set.of(), Set.of(), Set.of());
        var b = member(Set.of(), Set.of(한식),       Set.of(), Set.of(), Set.of());

        var menus = List.of(
                menu(김치찌개, 한식, Set.of()),   // A,B 둘 다 → (0.4+0.4)/2 = 0.4
                menu(초밥, 일식, Set.of())        // A만       → (0.4+0.0)/2 = 0.2
        );

        var ranked = service.scoreAndRank(List.of(a, b), menus, Map.of(), Map.of(), Set.of(), 10);

        assertThat(rankedIds(ranked)).containsExactly(김치찌개, 초밥);       // 더 많이 좋아하는 게 위
        assertThat(ranked.get(0).score()).isCloseTo(0.4, within(1e-9));   // 평균 반영
        assertThat(ranked.get(1).score()).isCloseTo(0.2, within(1e-9));
    }

    @Test
    void 방금_먹은_메뉴는_취향이_좋아도_밀린다() {
        UUID 제육 = UUID.randomUUID(), 김치찌개 = UUID.randomUUID();
        UUID 한식 = UUID.randomUUID();

        var member = member(Set.of(제육), Set.of(한식), Set.of(), Set.of(), Set.of());

        var menus = List.of(
                menu(제육, 한식, Set.of()),      // 메뉴 LIKE 1.0 …이지만 어제 먹음
                menu(김치찌개, 한식, Set.of())    // 카테고리 0.4, 안 먹음
        );
        var recency = Map.of(제육, new Recency(1L));   // 제육 어제 → 큰 감점

        var ranked = service.scoreAndRank(List.of(member), menus, recency, Map.of(), Set.of(), 10);

        assertThat(rankedIds(ranked)).containsExactly(김치찌개, 제육);   // 최근성이 우선순위를 뒤집음
    }

    @Test
    void EXCLUDE와_알레르기_메뉴는_다른_멤버가_좋아해도_랭킹에서_빠진다() {
        UUID 홍어 = UUID.randomUUID(), 땅콩과자 = UUID.randomUUID(), 김치찌개 = UUID.randomUUID();
        UUID 한식 = UUID.randomUUID(), 땅콩 = UUID.randomUUID();

        var liker = member(Set.of(홍어, 땅콩과자, 김치찌개), Set.of(), Set.of(), Set.of(), Set.of()); // 다 좋아함
        var picky = member(Set.of(), Set.of(), Set.of(홍어), Set.of(), Set.of(땅콩));               // 홍어 배제 + 땅콩 알레르기

        var menus = List.of(
                menu(홍어, 한식, Set.of()),          // picky 배제 → 제외
                menu(땅콩과자, 한식, Set.of(땅콩)),   // picky 알레르기 → 제외
                menu(김치찌개, 한식, Set.of())        // 남음
        );

        var ranked = service.scoreAndRank(List.of(liker, picky), menus, Map.of(), Map.of(), Set.of(), 10);

        assertThat(rankedIds(ranked)).containsExactly(김치찌개);   // 좋아하는 사람이 있어도 veto/알레르기가 우선
    }

    @Test
    void topN으로_상위만_남는다() {
        UUID 제육 = UUID.randomUUID(), 김치찌개 = UUID.randomUUID(), 초밥 = UUID.randomUUID();
        UUID 한식 = UUID.randomUUID(), 일식 = UUID.randomUUID();

        var member = member(Set.of(제육), Set.of(한식), Set.of(), Set.of(), Set.of());
        var menus = List.of(
                menu(제육, 한식, Set.of()),      // 1.0
                menu(김치찌개, 한식, Set.of()),   // 0.4
                menu(초밥, 일식, Set.of())        // 0.0
        );

        var ranked = service.scoreAndRank(List.of(member), menus, Map.of(), Map.of(), Set.of(), 2);

        assertThat(rankedIds(ranked)).containsExactly(제육, 김치찌개);   // 상위 2개만, 초밥 잘림
    }
}
