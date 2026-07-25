package com.gm.core.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.gm.core.domain.menu.model.Allergen;
import com.gm.core.domain.menu.repository.AllergenRepository;
import com.gm.core.domain.user.model.AllergenExtractionResult;
import com.gm.core.domain.user.model.ExtractedAllergen;
import com.gm.core.domain.user.port.AiChatPort;

/**
 * 화이트리스트 검증·정제 로직을 fake 포트로 검증한다. (외부 AI 호출 없음)
 */
class AllergenExtractionServiceTest {

    private static final UUID 우유_ID = UUID.randomUUID();
    private static final UUID 새우_ID = UUID.randomUUID();

    // 마스터 22종 중 2개만 있다고 가정
    private static final List<Allergen> MASTER = List.of(
            new Allergen(우유_ID, "우유", null),
            new Allergen(새우_ID, "새우", null)
    );

    /** AI가 주어진 이름들을 그대로 뱉는 fake. (프롬프트/HTTP 무관) */
    private AllergenExtractionService serviceReturning(String... aiNames) {
        AiChatPort fakeAi = (text, masterNames) -> new ExtractedAllergen(Arrays.asList(aiNames));
        return new AllergenExtractionService(fakeAi, fakeRepository());
    }

    private AllergenRepository fakeRepository() {
        return new AllergenRepository() {
            @Override
            public List<Allergen> findAll() {
                return MASTER;
            }

            @Override
            public Set<UUID> findExistingIds(Set<UUID> ids) {
                return MASTER.stream()
                        .map(Allergen::id)
                        .filter(ids::contains)
                        .collect(Collectors.toUnmodifiableSet());
            }
        };
    }

    private List<String> names(List<Allergen> allergens) {
        return allergens.stream().map(Allergen::name).toList();
    }

    @Test
    void 마스터에_있으면_표준으로_id까지_확정된다() {
        var result = serviceReturning("우유", "새우").extract("우유랑 새우 못 먹어요");

        assertThat(result.standardAllergens()).extracting(Allergen::id)
                .containsExactlyInAnyOrder(우유_ID, 새우_ID);
        assertThat(result.customAllergens()).isEmpty();
    }

    @Test
    void 마스터에_없으면_비표준_텍스트로_보존된다() {
        var result = serviceReturning("파인애플").extract("파인애플 알레르기");

        assertThat(result.standardAllergens()).isEmpty();
        assertThat(result.customAllergens()).containsExactly("파인애플");
    }

    @Test
    void 표준과_비표준이_섞여도_각각_분류된다() {
        var result = serviceReturning("우유", "파인애플").extract("우유, 파인애플");

        assertThat(names(result.standardAllergens())).containsExactly("우유");
        assertThat(result.customAllergens()).containsExactly("파인애플");
    }

    @Test
    void 대소문자_공백_차이가_있어도_마스터에_매칭된다() {
        // 공백·대소문자 정규화로 매칭돼야 한다
        var result = serviceReturning(" 우 유 ").extract("띄어쓴 우유");

        assertThat(names(result.standardAllergens())).containsExactly("우유");
        assertThat(result.customAllergens()).isEmpty();
    }

    @Test
    void null_과_공백_원소는_소실없이_안전하게_걸러진다() {
        // 모델이 null·공백을 뱉어도 어느 목록에도 남지 않아야 한다 (normalize(null) 통과 버그 방지)
        var result = serviceReturning("우유", null, "   ", "").extract("...");

        assertThat(names(result.standardAllergens())).containsExactly("우유");
        assertThat(result.customAllergens()).isEmpty();
    }

    @Test
    void 중복_이름은_한번만_담긴다() {
        var result = serviceReturning("우유", "우유", "파인애플", "파인애플").extract("...");

        assertThat(names(result.standardAllergens())).containsExactly("우유");
        assertThat(result.customAllergens()).containsExactly("파인애플");
    }

    @Test
    void 지나치게_긴_이름은_제외된다() {
        String tooLong = "가".repeat(31);   // 상한 30 초과
        var result = serviceReturning(tooLong, "새우").extract("...");

        assertThat(names(result.standardAllergens())).containsExactly("새우");
        assertThat(result.customAllergens()).isEmpty();
    }

    @Test
    void 비표준_개수는_상한까지만_담긴다() {
        // 상한 15 초과로 비정상 응답이 와도 15개까지만 (custom_allergen_text 500자 방어)
        String[] many = IntStream.range(0, 50).mapToObj(i -> "성분" + i).toArray(String[]::new);
        var result = serviceReturning(many).extract("...");

        assertThat(result.customAllergens()).hasSize(15);
    }

    @Test
    void 표준_매칭은_개수_상한이_없다() {
        // 매칭은 제한하지 않는다 — 마스터에 있는 건 모두 통과 (여기선 우유/새우 2개)
        var result = serviceReturning("우유", "새우").extract("...");
        assertThat(result.standardAllergens()).hasSize(2);
    }

    @Test
    void 콤마와_HTML_위험문자는_제거된다() {
        // 저장 컬럼이 콤마 구분이라 항목 내 콤마는 경계를 깬다 → 공백 치환. < > & " 도 제거.
        var result = serviceReturning("파인애플, 키위", "<script>").extract("...");

        // "파인애플, 키위"는 콤마가 공백으로 → 한 항목 "파인애플 키위"
        assertThat(result.customAllergens()).containsExactly("파인애플 키위", "script");
    }

    @Test
    void AI가_빈_결과를_주면_둘_다_비어있다() {
        var result = serviceReturning().extract("알레르기 없어요");

        assertThat(result.standardAllergens()).isEmpty();
        assertThat(result.customAllergens()).isEmpty();
    }

    @Test
    void allergenNames가_null이어도_안전하다() {
        AiChatPort nullAi = (text, masterNames) -> new ExtractedAllergen(null);
        var service = new AllergenExtractionService(nullAi, fakeRepository());

        AllergenExtractionResult result = service.extract("...");

        assertThat(result.standardAllergens()).isEmpty();
        assertThat(result.customAllergens()).isEmpty();
    }
}
