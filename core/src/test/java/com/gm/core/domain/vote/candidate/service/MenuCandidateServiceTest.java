package com.gm.core.domain.vote.candidate.service;

import java.util.List;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateErrorCode;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateException;
import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.RecommendedMenuCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.session.service.VoteSessionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MenuCandidateServiceTest {

    @Mock
    private GroupService groupService;

    @Mock
    private VoteSessionService voteSessionService;

    @Mock
    private VoteCandidateRepository voteCandidateRepository;

    @Test
    @DisplayName("추천 완료 시 메뉴 후보를 저장하고 세션을 메뉴 투표 상태로 변경한다")
    void completeRecommendation_savesCandidates_andMovesSessionToMenuVoting() {
        UUID voteSessionId = UUID.randomUUID();
        List<RecommendedMenuCandidate> recommendations = List.of(
                new RecommendedMenuCandidate(UUID.randomUUID(), 1, "따뜻한 국물 메뉴"),
                new RecommendedMenuCandidate(UUID.randomUUID(), 2, "그룹 선호도가 높은 메뉴")
        );
        given(voteCandidateRepository.saveNewCandidates(anyList()))
                .willAnswer(invocation -> invocation.getArgument(0));
        MenuCandidateService service = new MenuCandidateService(
                groupService,
                voteSessionService,
                voteCandidateRepository
        );

        List<VoteCandidate> saved = service.completeRecommendation(voteSessionId, recommendations);

        assertThat(saved).hasSize(2);
        ArgumentCaptor<List<VoteCandidate>> captor = ArgumentCaptor.captor();
        InOrder inOrder = inOrder(voteCandidateRepository, voteSessionService);
        inOrder.verify(voteCandidateRepository).saveNewCandidates(captor.capture());
        inOrder.verify(voteSessionService)
                .changeVoteSessionStatus(voteSessionId, VoteSessionStatus.MENU_VOTING);
        assertThat(captor.getValue())
                .allSatisfy(candidate -> {
                    assertThat(candidate.voteSessionId()).isEqualTo(voteSessionId);
                    assertThat(candidate.selected()).isFalse();
                    assertThat(candidate.goCount()).isZero();
                    assertThat(candidate.maybeCount()).isZero();
                    assertThat(candidate.noCount()).isZero();
                    assertThat(candidate.respondentCount()).isZero();
                    assertThat(candidate.resultStatus()).isEqualTo(VoteCandidateResult.PENDING);
                });
        assertThat(captor.getValue())
                .extracting(VoteCandidate::displayOrder)
                .containsExactly(1, 2);
    }

    @Test
    @DisplayName("추천 목록이 비어 있으면 후보를 저장하지 않는다")
    void completeRecommendation_withEmptyRecommendations_throwsInvalidRecommendations() {
        MenuCandidateService service = new MenuCandidateService(
                groupService,
                voteSessionService,
                voteCandidateRepository
        );

        assertThatThrownBy(() -> service.completeRecommendation(UUID.randomUUID(), List.of()))
                .isInstanceOfSatisfying(VoteCandidateException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(VoteCandidateErrorCode.INVALID_RECOMMENDATIONS));
        verifyNoInteractions(voteCandidateRepository, voteSessionService);
    }

    @Test
    @DisplayName("추천 후보가 10개를 초과하면 후보를 저장하지 않는다")
    void completeRecommendation_withMoreThanTenRecommendations_throwsInvalidRecommendations() {
        List<RecommendedMenuCandidate> recommendations = java.util.stream.IntStream.rangeClosed(1, 11)
                .mapToObj(order -> new RecommendedMenuCandidate(UUID.randomUUID(), order, "추천"))
                .toList();
        MenuCandidateService service = new MenuCandidateService(
                groupService,
                voteSessionService,
                voteCandidateRepository
        );

        assertThatThrownBy(() -> service.completeRecommendation(UUID.randomUUID(), recommendations))
                .isInstanceOfSatisfying(VoteCandidateException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(VoteCandidateErrorCode.INVALID_RECOMMENDATIONS));
        verifyNoInteractions(voteCandidateRepository, voteSessionService);
    }

    @Test
    @DisplayName("추천 목록에 같은 메뉴가 두 번 있으면 후보를 저장하지 않는다")
    void completeRecommendation_withDuplicateMenuId_throwsInvalidRecommendations() {
        UUID menuId = UUID.randomUUID();
        List<RecommendedMenuCandidate> recommendations = List.of(
                new RecommendedMenuCandidate(menuId, 1, "첫 번째"),
                new RecommendedMenuCandidate(menuId, 2, "두 번째")
        );
        MenuCandidateService service = new MenuCandidateService(
                groupService,
                voteSessionService,
                voteCandidateRepository
        );

        assertThatThrownBy(() -> service.completeRecommendation(UUID.randomUUID(), recommendations))
                .isInstanceOfSatisfying(VoteCandidateException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(VoteCandidateErrorCode.INVALID_RECOMMENDATIONS));
        verifyNoInteractions(voteCandidateRepository, voteSessionService);
    }

    @Test
    @DisplayName("추천 목록에 같은 노출 순서가 두 번 있으면 후보를 저장하지 않는다")
    void completeRecommendation_withDuplicateDisplayOrder_throwsInvalidRecommendations() {
        List<RecommendedMenuCandidate> recommendations = List.of(
                new RecommendedMenuCandidate(UUID.randomUUID(), 1, "첫 번째"),
                new RecommendedMenuCandidate(UUID.randomUUID(), 1, "두 번째")
        );
        MenuCandidateService service = new MenuCandidateService(
                groupService,
                voteSessionService,
                voteCandidateRepository
        );

        assertThatThrownBy(() -> service.completeRecommendation(UUID.randomUUID(), recommendations))
                .isInstanceOfSatisfying(VoteCandidateException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(VoteCandidateErrorCode.INVALID_RECOMMENDATIONS));
        verifyNoInteractions(voteCandidateRepository, voteSessionService);
    }

    @Test
    @DisplayName("세션 상태를 메뉴 투표로 변경할 수 없으면 상태 오류를 반환한다")
    void completeRecommendation_withInvalidSessionStatus_throwsInvalidSessionStatus() {
        UUID voteSessionId = UUID.randomUUID();
        List<RecommendedMenuCandidate> recommendations = List.of(
                new RecommendedMenuCandidate(UUID.randomUUID(), 1, "추천")
        );
        given(voteCandidateRepository.saveNewCandidates(anyList()))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(voteSessionService.changeVoteSessionStatus(voteSessionId, VoteSessionStatus.MENU_VOTING))
                .willThrow(new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS));
        MenuCandidateService service = new MenuCandidateService(
                groupService,
                voteSessionService,
                voteCandidateRepository
        );

        assertThatThrownBy(() -> service.completeRecommendation(voteSessionId, recommendations))
                .isInstanceOfSatisfying(VoteSessionException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(VoteSessionErrorCode.INVALID_SESSION_STATUS));
    }

    @Test
    @DisplayName("메뉴 후보는 노출 순서로 조회한다")
    void findMenuCandidates_returnsRepositoryResult() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        VoteSession session = VoteSession.builder()
                .id(voteSessionId)
                .diningGroupId(groupId)
                .voteSessionStatus(VoteSessionStatus.MENU_VOTING)
                .title("저녁 메뉴 투표")
                .build();
        MenuVoteCandidate candidate = new MenuVoteCandidate(
                UUID.randomUUID(),
                voteSessionId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "김치찌개",
                "한식",
                "https://example.com/kimchi.jpg",
                1,
                0,
                0,
                0,
                0,
                VoteCandidateResult.PENDING,
                "국물 메뉴 선호 반영"
        );
        given(voteSessionService.findVoteSession(voteSessionId)).willReturn(session);
        given(voteCandidateRepository.findAllByVoteSessionId(voteSessionId))
                .willReturn(List.of(candidate));
        MenuCandidateService service = new MenuCandidateService(
                groupService,
                voteSessionService,
                voteCandidateRepository
        );

        List<MenuVoteCandidate> result = service.findMenuCandidates(groupId, userId, voteSessionId);

        assertThat(result).containsExactly(candidate);
        then(groupService).should().findGroupDetail(groupId, userId);
    }

    @Test
    @DisplayName("경로 그룹과 세션의 그룹이 다르면 세션을 찾을 수 없는 것으로 처리한다")
    void findMenuCandidates_withDifferentSessionGroup_throwsSessionNotFound() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        VoteSession session = VoteSession.builder()
                .id(voteSessionId)
                .diningGroupId(UUID.randomUUID())
                .voteSessionStatus(VoteSessionStatus.MENU_VOTING)
                .title("저녁 메뉴 투표")
                .build();
        given(voteSessionService.findVoteSession(voteSessionId)).willReturn(session);
        MenuCandidateService service = new MenuCandidateService(
                groupService,
                voteSessionService,
                voteCandidateRepository
        );

        assertThatThrownBy(() -> service.findMenuCandidates(groupId, userId, voteSessionId))
                .isInstanceOfSatisfying(VoteSessionException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(VoteSessionErrorCode.SESSION_NOT_FOUND));
        verifyNoInteractions(voteCandidateRepository);
    }
}
