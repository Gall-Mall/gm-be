package com.gm.core.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.gm.core.domain.recommendation.model.CuratedCandidate;
import com.gm.core.domain.recommendation.model.CuratedMenu;
import com.gm.core.domain.recommendation.model.MenuCurationCommand;
import com.gm.core.domain.recommendation.port.MenuCurationPort;

/**
 * AI 큐레이션 결과의 화이트리스트(이름→id) 검증·순서 보존·상한 로직을 fake 포트로 검증한다.
 */
class RecommendationCurationServiceTest {

    private static final UUID 제육 = UUID.randomUUID();
    private static final UUID 초밥 = UUID.randomUUID();
    private static final UUID 파스타 = UUID.randomUUID();

    private Map<UUID, String> pool() {
        Map<UUID, String> pool = new LinkedHashMap<>();
        pool.put(제육, "제육볶음");
        pool.put(초밥, "초밥");
        pool.put(파스타, "파스타");
        return pool;
    }

    /** AI가 주어진 CuratedMenu 목록을 그대로 뱉는 fake. */
    private RecommendationCurationService serviceReturning(CuratedMenu... menus) {
        MenuCurationPort fake = command -> List.of(menus);
        return new RecommendationCurationService(fake);
    }

    private CuratedMenu menu(String name, String reason) {
        return new CuratedMenu(name, reason);
    }

    @Test
    void 후보에_있는_이름만_id로_확정되고_순서를_보존한다() {
        var service = serviceReturning(menu("파스타", "이유1"), menu("제육볶음", "이유2"));

        List<CuratedCandidate> result = service.curate(pool(), List.of(), List.of(), List.of(), 10);

        // AI가 준 순서(파스타 → 제육) 보존
        assertThat(result).extracting(CuratedCandidate::menuId).containsExactly(파스타, 제육);
        assertThat(result).extracting(CuratedCandidate::description).containsExactly("이유1", "이유2");
    }

    @Test
    void 후보_목록에_없는_이름은_버려진다() {
        // "치킨"은 후보 풀에 없음 → 환각으로 간주해 제외
        var service = serviceReturning(menu("치킨", "환각"), menu("초밥", "정상"));

        List<CuratedCandidate> result = service.curate(pool(), List.of(), List.of(), List.of(), 10);

        assertThat(result).extracting(CuratedCandidate::menuId).containsExactly(초밥);
    }

    @Test
    void 대소문자_공백_차이가_있어도_후보와_매칭된다() {
        var service = serviceReturning(menu(" 파 스 타 ", "정규화"));

        List<CuratedCandidate> result = service.curate(pool(), List.of(), List.of(), List.of(), 10);

        assertThat(result).extracting(CuratedCandidate::menuId).containsExactly(파스타);
    }

    @Test
    void 중복_id는_한번만_담긴다() {
        var service = serviceReturning(menu("초밥", "첫번째"), menu("초밥", "중복"));

        List<CuratedCandidate> result = service.curate(pool(), List.of(), List.of(), List.of(), 10);

        assertThat(result).extracting(CuratedCandidate::menuId).containsExactly(초밥);
        assertThat(result.get(0).description()).isEqualTo("첫번째");
    }

    @Test
    void 최대_개수_상한을_지킨다() {
        var service = serviceReturning(menu("제육볶음", "1"), menu("초밥", "2"), menu("파스타", "3"));

        List<CuratedCandidate> result = service.curate(pool(), List.of(), List.of(), List.of(), 2);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CuratedCandidate::menuId).containsExactly(제육, 초밥);
    }

    @Test
    void 비표준_알레르기_토큰이_이름에_든_후보는_결정론적으로_제외된다() {
        // AI가 지시를 놓치고 "파인애플" 든 메뉴를 반환해도, 후보 풀 사전 제외로 애초에 매칭 불가
        Map<UUID, String> pool = new LinkedHashMap<>();
        UUID 볶음밥 = UUID.randomUUID();
        pool.put(볶음밥, "파인애플볶음밥");
        pool.put(초밥, "초밥");
        var service = serviceReturning(menu("파인애플볶음밥", "AI가 놓침"), menu("초밥", "정상"));

        List<CuratedCandidate> result = service.curate(
                pool, List.of("파인애플"), List.of(), List.of(), 10);

        assertThat(result).extracting(CuratedCandidate::menuId).containsExactly(초밥);
    }

    @Test
    void 알레르기_텍스트가_콤마로_여러개여도_각각_제외된다() {
        Map<UUID, String> pool = new LinkedHashMap<>();
        UUID 키위주스 = UUID.randomUUID();
        pool.put(키위주스, "키위주스");
        pool.put(초밥, "초밥");
        var service = serviceReturning(menu("키위주스", "x"), menu("초밥", "o"));

        List<CuratedCandidate> result = service.curate(
                pool, List.of("파인애플, 키위"), List.of(), List.of(), 10);

        assertThat(result).extracting(CuratedCandidate::menuId).containsExactly(초밥);
    }

    @Test
    void AI가_빈_결과를_주면_빈_목록을_반환한다() {
        var service = serviceReturning();

        List<CuratedCandidate> result = service.curate(pool(), List.of(), List.of(), List.of(), 10);

        assertThat(result).isEmpty();
    }

    @Test
    void 포트가_null을_반환해도_안전하다() {
        MenuCurationPort nullPort = command -> null;
        var service = new RecommendationCurationService(nullPort);

        List<CuratedCandidate> result = service.curate(pool(), List.of(), List.of(), List.of(), 10);

        assertThat(result).isEmpty();
    }

    @Test
    void 소프트신호의_null_공백은_커맨드에서_걸러진다() {
        // 포트에 전달되는 command를 캡처해 정제 여부 확인
        MenuCurationCommand[] captured = new MenuCurationCommand[1];
        MenuCurationPort capturing = command -> {
            captured[0] = command;
            return List.of();
        };
        var service = new RecommendationCurationService(capturing);

        service.curate(pool(), java.util.Arrays.asList("우유", null, "  "), List.of(), null, 10);

        assertThat(captured[0].allergenExclusionTexts()).containsExactly("우유");
        assertThat(captured[0].excludeFoodTexts()).isEmpty();
        assertThat(captured[0].candidateMenuNames()).containsExactly("제육볶음", "초밥", "파스타");
    }
}
