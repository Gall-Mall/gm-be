package com.gm.core.domain.recommendation.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gm.core.domain.group.exception.GroupErrorCode;
import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.model.GroupDetail;
import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.menu.model.Menu;
import com.gm.core.domain.menu.repository.CategoryRepository;
import com.gm.core.domain.menu.repository.MenuRepository;
import com.gm.core.domain.recommendation.model.CuratedCandidate;
import com.gm.core.domain.recommendation.model.GroupSoftSignals;
import com.gm.core.domain.recommendation.model.ScoredMenu;
import com.gm.core.domain.recommendation.repository.RecommendationRepository;
import com.gm.core.domain.vote.candidate.model.menu.RecommendedMenuCandidate;
import com.gm.core.domain.vote.candidate.model.menu.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.menu.VoteCandidateResult;
import com.gm.core.domain.vote.candidate.repository.menu.VoteCandidateRepository;
import com.gm.core.domain.vote.candidate.service.menu.MenuCandidateService;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.service.VoteSessionService;
import com.gm.core.event.EventPublisher;
import com.gm.core.event.payload.SurveyRequested;
import com.gm.core.transaction.AfterCommitExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MenuRecommendationServiceTest {

    @Mock
    private GroupService groupService;

    @Mock
    private VoteSessionService voteSessionService;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private RecommendationCurationService recommendationCurationService;

    @Mock
    private MenuCandidateService menuCandidateService;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private VoteCandidateRepository voteCandidateRepository;

    @Mock
    private EventPublisher eventPublisher;

    /** 트랜잭션이 없으면 즉시 실행되므로 실제 구현을 쓴다. */
    @Spy
    private AfterCommitExecutor afterCommitExecutor = new AfterCommitExecutor();

    @InjectMocks
    private MenuRecommendationService menuRecommendationService;

    private final UUID groupId = UUID.randomUUID();
    private final UUID voteSessionId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    // ---------- start ----------

    @Test
    @DisplayName("방장이 선호 입력 단계에서 시작하면 추천 단계로 전이하고 요청 이벤트를 발행한다")
    void start_transitionsAndPublishes() {
        givenSession(VoteSessionStatus.PREFERENCE_INPUT);
        givenRole(GroupMemberRole.OWNER);

        menuRecommendationService.start(ownerId, voteSessionId);

        // 전이를 여기서 해야 같은 요청이 두 번 들어와도 두 번째가 상태 검증에 걸린다.
        verify(voteSessionService)
                .changeVoteSessionStatus(voteSessionId, VoteSessionStatus.MENU_RECOMMENDING);
        verify(eventPublisher).publish(new SurveyRequested(groupId, voteSessionId));
    }

    @Test
    @DisplayName("실패한 세션은 FAILED로 표시해 새 세션을 만들게 한다")
    void markFailed_transitionsToFailed() {
        menuRecommendationService.markFailed(voteSessionId);

        verify(voteSessionService)
                .changeVoteSessionStatus(voteSessionId, VoteSessionStatus.FAILED);
    }

    @Test
    @DisplayName("방장이 아니면 추천을 시작할 수 없다")
    void start_rejectsNonOwner() {
        givenSession(VoteSessionStatus.PREFERENCE_INPUT);
        givenRole(GroupMemberRole.MEMBER);

        assertThatThrownBy(() -> menuRecommendationService.start(ownerId, voteSessionId))
                .isInstanceOf(GroupException.class)
                .hasFieldOrPropertyWithValue("errorCode", GroupErrorCode.NOT_GROUP_OWNER);

        verify(voteSessionService, org.mockito.Mockito.never())
                .changeVoteSessionStatus(any(), any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("선호 입력 단계가 아니면 추천을 시작할 수 없다")
    void start_rejectsWrongStatus() {
        givenSession(VoteSessionStatus.MENU_VOTING);
        givenRole(GroupMemberRole.OWNER);

        assertThatThrownBy(() -> menuRecommendationService.start(ownerId, voteSessionId))
                .isInstanceOf(VoteSessionException.class)
                .hasFieldOrPropertyWithValue("errorCode", VoteSessionErrorCode.INVALID_SESSION_STATUS);

        verify(voteSessionService, org.mockito.Mockito.never())
                .changeVoteSessionStatus(any(), any());
    }

    // ---------- generate ----------

    @Test
    @DisplayName("AI 큐레이션 순서대로 노출 순서를 매겨 후보를 저장한다")
    void generate_savesCuratedCandidatesInOrder() {
        givenMatchingSession();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        givenScored(List.of(first, second));
        givenMenuMaster(Map.of(first, "삼겹살", second, "냉면"));
        givenSoftSignals();
        given(recommendationCurationService.curate(any(), anyList(), anyList(), anyList(), anyInt()))
                .willReturn(List.of(
                        new CuratedCandidate(second, "시원해서"),
                        new CuratedCandidate(first, "든든해서")));

        menuRecommendationService.generate(groupId, voteSessionId);

        List<RecommendedMenuCandidate> saved = captureSaved();
        assertThat(saved).hasSize(2);
        // AI가 준 순서가 추천 순위다. displayOrder는 1부터 매긴다.
        assertThat(saved.get(0).menuId()).isEqualTo(second);
        assertThat(saved.get(0).displayOrder()).isEqualTo(1);
        assertThat(saved.get(0).description()).isEqualTo("시원해서");
        assertThat(saved.get(1).menuId()).isEqualTo(first);
        assertThat(saved.get(1).displayOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("투표 세션의 오늘 키워드를 AI 큐레이션 신호의 최우선 항목으로 전달한다")
    void generate_prependsVoteSessionKeywordsToCurationSignals() {
        givenMatchingSession("매콤한, 국물", "면 요리");
        UUID menuId = UUID.randomUUID();
        givenScored(List.of(menuId));
        givenMenuMaster(Map.of(menuId, "김치찌개"));
        given(recommendationRepository.findSoftSignalsByGroupId(groupId))
                .willReturn(new GroupSoftSignals(
                        List.of(),
                        List.of("든든한 음식"),
                        List.of("날것")
                ));
        given(recommendationCurationService.curate(any(), anyList(), anyList(), anyList(), anyInt()))
                .willReturn(List.of(new CuratedCandidate(menuId, "오늘의 매콤한 국물 취향 반영")));

        menuRecommendationService.generate(groupId, voteSessionId);

        verify(recommendationService).recommend(groupId, Set.of(), 50);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> preferenceCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> dislikeCaptor = ArgumentCaptor.forClass(List.class);
        verify(recommendationCurationService).curate(
                any(),
                eq(List.of()),
                preferenceCaptor.capture(),
                dislikeCaptor.capture(),
                eq(10)
        );
        assertThat(preferenceCaptor.getValue()).containsExactly(
                "오늘의 핵심 선호: 매콤한, 국물",
                "든든한 음식"
        );
        assertThat(dislikeCaptor.getValue()).containsExactly(
                "오늘의 핵심 비선호: 면 요리",
                "날것"
        );
    }

    @Test
    @DisplayName("재추천은 이전 라운드 메뉴를 결정론 후보에서 제외한다")
    void generate_excludesPreviousRoundMenus() {
        givenMatchingSession();
        UUID previousMenuId = UUID.randomUUID();
        UUID freshMenuId = UUID.randomUUID();
        given(voteCandidateRepository.findAllByVoteSessionId(voteSessionId))
                .willReturn(List.of(new MenuVoteCandidate(
                        UUID.randomUUID(),
                        voteSessionId,
                        previousMenuId,
                        UUID.randomUUID(),
                        "이전 메뉴",
                        "한식",
                        null,
                        1,
                        0,
                        0,
                        1,
                        1,
                        VoteCandidateResult.KEPT,
                        null
                )));
        given(recommendationService.recommend(
                eq(groupId), any(), eq(50)))
                .willReturn(List.of(new ScoredMenu(freshMenuId, 1.0)));
        givenMenuMaster(Map.of(freshMenuId, "새 메뉴"));
        givenSoftSignals();
        given(recommendationCurationService.curate(any(), anyList(), anyList(), anyList(), anyInt()))
                .willReturn(List.of(new CuratedCandidate(freshMenuId, "새로운 후보")));

        menuRecommendationService.generate(groupId, voteSessionId);

        verify(recommendationService).recommend(groupId, Set.of(previousMenuId), 50);
        assertThat(captureSaved())
                .extracting(RecommendedMenuCandidate::menuId)
                .containsExactly(freshMenuId);
    }

    @Test
    @DisplayName("AI가 후보를 못 고르면 결정론 상위 순서로 대체한다")
    void generate_fallsBackToDeterministicOrderWhenCurationIsEmpty() {
        givenMatchingSession();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        givenScored(List.of(first, second));
        givenMenuMaster(Map.of(first, "삼겹살", second, "냉면"));
        givenSoftSignals();
        given(recommendationCurationService.curate(any(), anyList(), anyList(), anyList(), anyInt()))
                .willReturn(List.of());

        menuRecommendationService.generate(groupId, voteSessionId);

        List<RecommendedMenuCandidate> saved = captureSaved();
        // AI 응답 하나로 세션을 멈추지 않는다. 추천 이유만 비운다.
        assertThat(saved).extracting(RecommendedMenuCandidate::menuId).containsExactly(first, second);
        assertThat(saved).allSatisfy(candidate -> assertThat(candidate.description()).isNull());
    }

    @Test
    @DisplayName("대체 후보도 최종 후보 수를 넘기지 않는다")
    void generate_fallbackRespectsFinalCandidateLimit() {
        givenMatchingSession();
        List<UUID> menuIds = IntStream.range(0, 15).mapToObj(i -> UUID.randomUUID()).toList();
        givenScored(menuIds);
        givenMenuMaster(menuIds.stream()
                .collect(java.util.stream.Collectors.toMap(id -> id, id -> "메뉴-" + id)));
        givenSoftSignals();
        given(recommendationCurationService.curate(any(), anyList(), anyList(), anyList(), anyInt()))
                .willReturn(List.of());

        menuRecommendationService.generate(groupId, voteSessionId);

        assertThat(captureSaved()).hasSize(10);
    }

    @Test
    @DisplayName("마스터에 없는 메뉴는 AI 후보 풀에서 제외한다")
    void generate_dropsMenusMissingFromMaster() {
        givenMatchingSession();
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        givenScored(List.of(known, unknown));
        givenMenuMaster(Map.of(known, "삼겹살"));
        givenSoftSignals();
        given(recommendationCurationService.curate(any(), anyList(), anyList(), anyList(), anyInt()))
                .willReturn(List.of(new CuratedCandidate(known, "든든해서")));

        menuRecommendationService.generate(groupId, voteSessionId);

        ArgumentCaptor<Map<UUID, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(recommendationCurationService)
                .curate(captor.capture(), anyList(), anyList(), anyList(), anyInt());
        assertThat(captor.getValue()).containsOnlyKeys(known);
    }

    @Test
    @DisplayName("추천할 메뉴가 없으면 후보를 저장하지 않는다")
    void generate_rejectsWhenNoCandidate() {
        givenMatchingSession();
        given(recommendationService.recommend(eq(groupId), any(), anyInt())).willReturn(List.of());

        assertThatThrownBy(() -> menuRecommendationService.generate(groupId, voteSessionId))
                .isInstanceOf(VoteSessionException.class);

        verifyNoInteractions(menuCandidateService);
    }

    // ---------- helpers ----------

    private void givenSession(VoteSessionStatus status) {
        given(voteSessionService.findVoteSession(voteSessionId)).willReturn(VoteSession.builder()
                .id(voteSessionId)
                .diningGroupId(groupId)
                .voteSessionStatus(status)
                .title("점심 메뉴 투표")
                .build());
    }

    private void givenRole(GroupMemberRole role) {
        given(groupService.findGroupDetail(groupId, ownerId))
                .willReturn(new GroupDetail(null, role));
    }

    /** generate는 세션의 groupId와 payload의 groupId를 대조한다. */
    private void givenMatchingSession() {
        givenSession(VoteSessionStatus.MENU_RECOMMENDING);
    }

    private void givenMatchingSession(String likeKeyword, String dislikeKeyword) {
        given(voteSessionService.findVoteSession(voteSessionId)).willReturn(VoteSession.builder()
                .id(voteSessionId)
                .diningGroupId(groupId)
                .voteSessionStatus(VoteSessionStatus.MENU_RECOMMENDING)
                .title("점심 메뉴 투표")
                .likeKeyword(likeKeyword)
                .dislikeKeyword(dislikeKeyword)
                .build());
    }

    private void givenScored(List<UUID> menuIds) {
        given(recommendationService.recommend(eq(groupId), eq(Set.of()), anyInt()))
                .willReturn(menuIds.stream().map(id -> new ScoredMenu(id, 1.0)).toList());
    }

    private void givenMenuMaster(Map<UUID, String> namesById) {
        given(menuRepository.findAll()).willReturn(namesById.entrySet().stream()
                .map(entry -> new Menu(entry.getKey(), UUID.randomUUID(), entry.getValue(), null))
                .toList());
    }

    private void givenSoftSignals() {
        given(recommendationRepository.findSoftSignalsByGroupId(groupId))
                .willReturn(GroupSoftSignals.empty());
    }

    private List<RecommendedMenuCandidate> captureSaved() {
        ArgumentCaptor<List<RecommendedMenuCandidate>> captor = ArgumentCaptor.forClass(List.class);
        verify(menuCandidateService).completeRecommendation(eq(voteSessionId), captor.capture());
        return captor.getValue();
    }
}
