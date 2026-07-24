package com.gm.integration.vote.candidate;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.gm.api.ApiApplication;
import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.RecommendedMenuCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.candidate.service.MenuCandidateService;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.db.domain.menu.category.entity.FoodCategoryEntity;
import com.gm.db.domain.menu.menu.entity.MenuEntity;
import com.gm.db.domain.vote.session.entity.VoteSessionEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ApiApplication.class)
@Transactional
class MenuCandidateRepositoryIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VoteCandidateRepository voteCandidateRepository;

    @Autowired
    private MenuCandidateService menuCandidateService;

    @Test
    @DisplayName("추천 후보를 저장하고 메뉴·카테고리 정보와 함께 노출 순서로 조회한다")
    void saveAndFindMenuCandidates() {
        FoodCategoryEntity category = new FoodCategoryEntity();
        ReflectionTestUtils.setField(category, "name", "한식");
        entityManager.persist(category);

        MenuEntity menu = new MenuEntity();
        ReflectionTestUtils.setField(menu, "categoryId", category.getId());
        ReflectionTestUtils.setField(menu, "name", "김치찌개");
        ReflectionTestUtils.setField(menu, "imageUrl", "https://example.com/kimchi.jpg");
        entityManager.persist(menu);

        UUID voteSessionId = UUID.randomUUID();
        VoteCandidate candidate = VoteCandidate.builder()
                .voteSessionId(voteSessionId)
                .menuId(menu.getId())
                .displayOrder(1)
                .selected(false)
                .goCount(0)
                .maybeCount(0)
                .noCount(0)
                .respondentCount(0)
                .resultStatus(VoteCandidateResult.PENDING)
                .description("국물 메뉴 선호 반영")
                .build();

        List<VoteCandidate> saved = voteCandidateRepository.saveNewCandidates(List.of(candidate));
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        List<MenuVoteCandidate> result = voteCandidateRepository
                .findAllByVoteSessionId(voteSessionId);

        assertThat(saved).singleElement().extracting(VoteCandidate::id).isNotNull();
        assertThat(result).singleElement().satisfies(found -> {
            assertThat(found.voteCandidateId()).isEqualTo(saved.get(0).id());
            assertThat(found.menuName()).isEqualTo("김치찌개");
            assertThat(found.categoryName()).isEqualTo("한식");
            assertThat(found.imageUrl()).isEqualTo("https://example.com/kimchi.jpg");
            assertThat(found.displayOrder()).isEqualTo(1);
            assertThat(found.resultStatus()).isEqualTo(VoteCandidateResult.PENDING);
        });
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("추천 완료 처리는 후보 저장과 MENU_VOTING 상태 전환을 함께 반영한다")
    void completeRecommendation_persistsCandidatesAndStatus() {
        FoodCategoryEntity category = new FoodCategoryEntity();
        ReflectionTestUtils.setField(category, "name", "일식");
        entityManager.persist(category);

        MenuEntity menu = new MenuEntity();
        ReflectionTestUtils.setField(menu, "categoryId", category.getId());
        ReflectionTestUtils.setField(menu, "name", "돈가스");
        entityManager.persist(menu);

        VoteSessionEntity session = new VoteSessionEntity(
                UUID.randomUUID(),
                VoteSessionStatus.MENU_RECOMMENDING,
                "점심 메뉴 투표",
                null,
                null,
                null,
                null
        );
        entityManager.persist(session);
        entityManager.flush();

        menuCandidateService.completeRecommendation(
                session.getId(),
                List.of(new RecommendedMenuCandidate(menu.getId(), 1, "선호 카테고리 반영"))
        );
        entityManager.flush();
        entityManager.clear();

        VoteSessionEntity updatedSession = entityManager.find(VoteSessionEntity.class, session.getId());
        List<MenuVoteCandidate> candidates = voteCandidateRepository
                .findAllByVoteSessionId(session.getId());
        assertThat(updatedSession.getVoteSessionStatus()).isEqualTo(VoteSessionStatus.MENU_VOTING);
        assertThat(candidates).singleElement().satisfies(found -> {
            assertThat(found.menuName()).isEqualTo("돈가스");
            assertThat(found.description()).isEqualTo("선호 카테고리 반영");
        });
    }
}
