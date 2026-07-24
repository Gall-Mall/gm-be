package com.gm.core.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.gm.core.domain.menu.model.Category;
import com.gm.core.domain.menu.repository.CategoryRepository;
import com.gm.core.domain.user.model.ExtractedFoodPreference;
import com.gm.core.domain.user.port.FoodPreferenceAiPort;

/**
 * 카테고리 화이트리스트 매칭·잔여 텍스트·정제 로직을 fake 포트로 검증한다. (외부 AI 호출 없음)
 */
class FoodPreferenceExtractionServiceTest {

    private static final UUID 한식_ID = UUID.randomUUID();
    private static final UUID 양식_ID = UUID.randomUUID();

    private static final List<Category> MASTER = List.of(
            new Category(한식_ID, "한식"),
            new Category(양식_ID, "양식")
    );

    private FoodPreferenceExtractionService serviceReturning(String... aiKeywords) {
        FoodPreferenceAiPort fakeAi = (text, categoryNames) -> new ExtractedFoodPreference(Arrays.asList(aiKeywords));
        CategoryRepository fakeRepo = () -> MASTER;
        return new FoodPreferenceExtractionService(fakeAi, fakeRepo);
    }

    private List<String> names(List<Category> categories) {
        return categories.stream().map(Category::name).toList();
    }

    @Test
    void 마스터에_있으면_카테고리로_id까지_확정된다() {
        var result = serviceReturning("한식", "양식").extract("한식이랑 양식 좋아요");

        assertThat(result.matchedCategories()).extracting(Category::id)
                .containsExactlyInAnyOrder(한식_ID, 양식_ID);
        assertThat(result.unmatchedText()).isEmpty();
    }

    @Test
    void 마스터에_없으면_잔여_텍스트로_보존된다() {
        var result = serviceReturning("매콤한 국물").extract("매콤한 국물 좋아해요");

        assertThat(result.matchedCategories()).isEmpty();
        assertThat(result.unmatchedText()).isEqualTo("매콤한 국물");
    }

    @Test
    void 카테고리와_잔여취향이_섞이면_각각_분류된다() {
        var result = serviceReturning("한식", "매콤한 국물", "느끼한 거").extract("...");

        assertThat(names(result.matchedCategories())).containsExactly("한식");
        assertThat(result.unmatchedText()).isEqualTo("매콤한 국물, 느끼한 거");
    }

    @Test
    void 대소문자_공백_차이가_있어도_카테고리에_매칭된다() {
        var result = serviceReturning(" 한 식 ").extract("띄어쓴 한식");

        assertThat(names(result.matchedCategories())).containsExactly("한식");
        assertThat(result.unmatchedText()).isEmpty();
    }

    @Test
    void null_과_공백_원소는_소실없이_걸러진다() {
        var result = serviceReturning("한식", null, "   ", "").extract("...");

        assertThat(names(result.matchedCategories())).containsExactly("한식");
        assertThat(result.unmatchedText()).isEmpty();
    }

    @Test
    void 중복_키워드는_한번만_반영된다() {
        var result = serviceReturning("한식", "한식", "매운맛", "매운맛").extract("...");

        assertThat(names(result.matchedCategories())).containsExactly("한식");
        assertThat(result.unmatchedText()).isEqualTo("매운맛");
    }

    @Test
    void 지나치게_긴_키워드는_제외된다() {
        String tooLong = "매".repeat(31);
        var result = serviceReturning(tooLong, "한식").extract("...");

        assertThat(names(result.matchedCategories())).containsExactly("한식");
        assertThat(result.unmatchedText()).isEmpty();
    }

    @Test
    void AI가_빈_결과를_주면_카테고리와_텍스트가_모두_비어있다() {
        var result = serviceReturning().extract("특별히 없어요");

        assertThat(result.matchedCategories()).isEmpty();
        assertThat(result.unmatchedText()).isEmpty();
    }

    @Test
    void foodKeywords가_null이어도_안전하다() {
        FoodPreferenceAiPort nullAi = (text, categoryNames) -> new ExtractedFoodPreference(null);
        var service = new FoodPreferenceExtractionService(nullAi, () -> MASTER);

        var result = service.extract("...");

        assertThat(result.matchedCategories()).isEmpty();
        assertThat(result.unmatchedText()).isEmpty();
    }
}
