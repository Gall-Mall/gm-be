package com.gm.core.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.gm.core.domain.menu.model.Category;
import com.gm.core.domain.menu.model.Menu;
import com.gm.core.domain.menu.repository.CategoryRepository;
import com.gm.core.domain.menu.repository.MenuRepository;
import com.gm.core.domain.user.model.ExtractedFoodPreference;
import com.gm.core.domain.user.model.FoodPreferencePolarity;
import com.gm.core.domain.user.port.FoodPreferenceAiPort;

/**
 * 메뉴 화이트리스트 매칭·잔여 텍스트·정제 로직을 fake 포트로 검증한다. (외부 AI 호출 없음)
 */
class FoodPreferenceExtractionServiceTest {

    private static final UUID 파스타_ID = UUID.randomUUID();
    private static final UUID 칼국수_ID = UUID.randomUUID();
    private static final UUID 카테고리_ID = UUID.randomUUID();

    // 메뉴 마스터 (id, categoryId, name, imageUrl)
    private static final List<Menu> MASTER = List.of(
            new Menu(파스타_ID, 카테고리_ID, "파스타", null),
            new Menu(칼국수_ID, 카테고리_ID, "칼국수", null)
    );

    private static final UUID 한식_ID = UUID.randomUUID();

    // 카테고리 마스터
    private static final List<Category> CATEGORY_MASTER = List.of(
            new Category(카테고리_ID, "양식"),
            new Category(한식_ID, "한식")
    );

    private static CategoryRepository fakeCategoryRepository() {
        return new CategoryRepository() {
            @Override public List<Category> findAll() { return CATEGORY_MASTER; }
            @Override public Set<UUID> findExistingIds(Set<UUID> ids) {
                return CATEGORY_MASTER.stream()
                        .map(Category::id)
                        .filter(ids::contains)
                        .collect(Collectors.toUnmodifiableSet());
            }
        };
    }

    private FoodPreferenceExtractionService serviceReturning(String... aiKeywords) {
        FoodPreferenceAiPort fakeAi = (text, menuNames, polarity) -> new ExtractedFoodPreference(Arrays.asList(aiKeywords));
        MenuRepository fakeRepo = new MenuRepository() {
            @Override public List<Menu> findAll() { return MASTER; }
            @Override public List<Menu> findMenusByCategoryId(UUID categoryId) { return List.of(); }
            @Override public Set<UUID> findExistingIds(Set<UUID> ids) {
                return MASTER.stream()
                        .map(Menu::id)
                        .filter(ids::contains)
                        .collect(Collectors.toUnmodifiableSet());
            }
        };
        return new FoodPreferenceExtractionService(fakeAi, fakeRepo, fakeCategoryRepository());
    }

    private List<String> names(List<Menu> menus) {
        return menus.stream().map(Menu::name).toList();
    }

    @Test
    void 마스터에_있으면_메뉴로_id까지_확정된다() {
        // "파스타 좋아함" → "파스타" (카테고리 양식으로 일반화 X)
        var result = serviceReturning("파스타", "칼국수").extract("파스타랑 칼국수 좋아요", FoodPreferencePolarity.EXCLUDE);

        assertThat(result.matchedMenus()).extracting(Menu::id)
                .containsExactlyInAnyOrder(파스타_ID, 칼국수_ID);
        assertThat(result.unmatchedText()).isEmpty();
    }

    @Test
    void 마스터에_없으면_잔여_텍스트로_보존된다() {
        var result = serviceReturning("매콤한 국물").extract("매콤한 국물 좋아해요", FoodPreferencePolarity.EXCLUDE);

        assertThat(result.matchedMenus()).isEmpty();
        assertThat(result.unmatchedText()).isEqualTo("매콤한 국물");
    }

    @Test
    void 구체적_메뉴와_잔여취향이_섞이면_각각_분류된다() {
        var result = serviceReturning("칼국수", "매콤한 국물", "느끼한 거").extract("...", FoodPreferencePolarity.EXCLUDE);

        assertThat(names(result.matchedMenus())).containsExactly("칼국수");
        assertThat(result.unmatchedText()).isEqualTo("매콤한 국물, 느끼한 거");
    }

    @Test
    void 대소문자_공백_차이가_있어도_메뉴에_매칭된다() {
        var result = serviceReturning(" 파 스 타 ").extract("띄어쓴 파스타", FoodPreferencePolarity.EXCLUDE);

        assertThat(names(result.matchedMenus())).containsExactly("파스타");
        assertThat(result.unmatchedText()).isEmpty();
    }

    @Test
    void null_과_공백_원소는_소실없이_걸러진다() {
        var result = serviceReturning("파스타", null, "   ", "").extract("...", FoodPreferencePolarity.EXCLUDE);

        assertThat(names(result.matchedMenus())).containsExactly("파스타");
        assertThat(result.unmatchedText()).isEmpty();
    }

    @Test
    void 중복_키워드는_한번만_반영된다() {
        var result = serviceReturning("파스타", "파스타", "매운맛", "매운맛").extract("...", FoodPreferencePolarity.EXCLUDE);

        assertThat(names(result.matchedMenus())).containsExactly("파스타");
        assertThat(result.unmatchedText()).isEqualTo("매운맛");
    }

    @Test
    void 지나치게_긴_키워드는_제외된다() {
        String tooLong = "매".repeat(31);
        var result = serviceReturning(tooLong, "파스타").extract("...", FoodPreferencePolarity.EXCLUDE);

        assertThat(names(result.matchedMenus())).containsExactly("파스타");
        assertThat(result.unmatchedText()).isEmpty();
    }

    @Test
    void AI가_빈_결과를_주면_메뉴와_텍스트가_모두_비어있다() {
        var result = serviceReturning().extract("특별히 없어요", FoodPreferencePolarity.EXCLUDE);

        assertThat(result.matchedMenus()).isEmpty();
        assertThat(result.unmatchedText()).isEmpty();
    }

    @Test
    void 항목_내_콤마는_공백으로_치환돼_저장_경계를_지킨다() {
        // AI가 "짜장면, 짬뽕"을 한 항목으로 반환해도, 콤마 조인 저장 시 경계가 깨지면 안 된다.
        var result = serviceReturning("짜장면, 짬뽕").extract("...", FoodPreferencePolarity.EXCLUDE);

        // 콤마 → 공백 → 한 항목 "짜장면 짬뽕" (메뉴 미매칭이므로 텍스트로)
        assertThat(result.matchedMenus()).isEmpty();
        assertThat(result.unmatchedText()).isEqualTo("짜장면 짬뽕");
    }

    @Test
    void HTML_위험문자는_제거된다() {
        var result = serviceReturning("<b>매운</b>").extract("...", FoodPreferencePolarity.EXCLUDE);
        assertThat(result.unmatchedText()).isEqualTo("b 매운 /b");
    }

    @Test
    void 잔여텍스트는_500자를_넘지_않는다() {
        // 30자 × 15개 초과로 비정상 응답이 와도 저장 컬럼(500) 이내
        String[] many = java.util.stream.IntStream.range(0, 30)
                .mapToObj(i -> "가".repeat(25) + i).toArray(String[]::new);
        var result = serviceReturning(many).extract("...", FoodPreferencePolarity.EXCLUDE);

        assertThat(result.unmatchedText().length()).isLessThanOrEqualTo(500);
    }

    @Test
    void 카테고리_이름은_텍스트가_아니라_카테고리로_id까지_확정된다() {
        // "한식 싫어요" → 텍스트로 흘리면 추천 스코어링의 카테고리 가중치가 적용되지 않는다.
        var result = serviceReturning("한식").extract("한식은 좀 그래요", FoodPreferencePolarity.EXCLUDE);

        assertThat(result.matchedCategories()).extracting(Category::id).containsExactly(한식_ID);
        assertThat(result.matchedMenus()).isEmpty();
        assertThat(result.unmatchedText()).isEmpty();
    }

    @Test
    void 메뉴와_카테고리와_잔여취향이_섞이면_각각_분류된다() {
        var result = serviceReturning("파스타", "한식", "매콤한 국물").extract("...", FoodPreferencePolarity.EXCLUDE);

        assertThat(names(result.matchedMenus())).containsExactly("파스타");
        assertThat(result.matchedCategories()).extracting(Category::name).containsExactly("한식");
        assertThat(result.unmatchedText()).isEqualTo("매콤한 국물");
    }

    @Test
    void 요청한_극성이_AI_포트까지_전달된다() {
        // 극성이 전달되지 않으면 한 문장에 좋아함·싫어함이 섞였을 때 통째로 한쪽에 저장된다.
        var 전달된극성 = new java.util.concurrent.atomic.AtomicReference<FoodPreferencePolarity>();
        FoodPreferenceAiPort spyAi = (text, menuNames, polarity) -> {
            전달된극성.set(polarity);
            return new ExtractedFoodPreference(List.of());
        };
        MenuRepository repo = new MenuRepository() {
            @Override public List<Menu> findAll() { return MASTER; }
            @Override public List<Menu> findMenusByCategoryId(UUID categoryId) { return List.of(); }
            @Override public Set<UUID> findExistingIds(Set<UUID> ids) { return Set.of(); }
        };
        var service = new FoodPreferenceExtractionService(spyAi, repo, fakeCategoryRepository());

        service.extract("면요리는 싫고 국물은 좋아요", FoodPreferencePolarity.LIKE);

        assertThat(전달된극성.get()).isEqualTo(FoodPreferencePolarity.LIKE);
    }

    @Test
    void 매칭_메뉴는_개수_상한이_없다() {
        var result = serviceReturning("파스타", "칼국수").extract("...", FoodPreferencePolarity.EXCLUDE);
        assertThat(result.matchedMenus()).hasSize(2);
    }

    @Test
    void foodKeywords가_null이어도_안전하다() {
        FoodPreferenceAiPort nullAi = (text, menuNames, polarity) -> new ExtractedFoodPreference(null);
        MenuRepository repo = new MenuRepository() {
            @Override public List<Menu> findAll() { return MASTER; }
            @Override public List<Menu> findMenusByCategoryId(UUID categoryId) { return List.of(); }
            @Override public Set<UUID> findExistingIds(Set<UUID> ids) {
                return MASTER.stream()
                        .map(Menu::id)
                        .filter(ids::contains)
                        .collect(Collectors.toUnmodifiableSet());
            }
        };
        var service = new FoodPreferenceExtractionService(nullAi, repo, fakeCategoryRepository());

        var result = service.extract("...", FoodPreferencePolarity.EXCLUDE);

        assertThat(result.matchedMenus()).isEmpty();
        assertThat(result.unmatchedText()).isEmpty();
    }
}
