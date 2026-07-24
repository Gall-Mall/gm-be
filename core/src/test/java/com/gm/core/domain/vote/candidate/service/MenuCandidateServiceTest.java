package com.gm.core.domain.vote.candidate.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.RecommendedMenuCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class MenuCandidateServiceTest {

    @Mock
    private VoteSessionRepository voteSessionRepository;

    @Mock
    private VoteCandidateRepository voteCandidateRepository;

    @Test
    @DisplayName("추천 완료 시 메뉴 후보를 저장하고 세션을 메뉴 투표 상태로 변경한다")
    void completeRecommendation_savesCandidates_andMovesSessionToMenuVoting() {
        UUID voteSessionId = UUID.randomUUID();
        VoteSession recommendingSession = VoteSession.builder()
                .id(voteSessionId)
                .diningGroupId(UUID.randomUUID())
                .voteSessionStatus(VoteSessionStatus.MENU_RECOMMENDING)
                .title("저녁 메뉴 투표")
                .build();
        List<RecommendedMenuCandidate> recommendations = List.of(
                new RecommendedMenuCandidate(UUID.randomUUID(), 1, "따뜻한 국물 메뉴"),
                new RecommendedMenuCandidate(UUID.randomUUID(), 2, "그룹 선호도가 높은 메뉴")
        );
        given(voteSessionRepository.findById(voteSessionId)).willReturn(Optional.of(recommendingSession));
        given(voteCandidateRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(voteSessionRepository.updateStatus(voteSessionId, VoteSessionStatus.MENU_VOTING))
                .willReturn(Optional.of(recommendingSession.changeStatus(VoteSessionStatus.MENU_VOTING)));
        MenuCandidateService service = new MenuCandidateService(
                voteSessionRepository,
                voteCandidateRepository
        );

        List<VoteCandidate> saved = service.completeRecommendation(voteSessionId, recommendations);

        assertThat(saved).hasSize(2);
        ArgumentCaptor<List<VoteCandidate>> captor = ArgumentCaptor.forClass(List.class);
        InOrder inOrder = inOrder(voteCandidateRepository, voteSessionRepository);
        inOrder.verify(voteCandidateRepository).saveAll(captor.capture());
        inOrder.verify(voteSessionRepository).updateStatus(voteSessionId, VoteSessionStatus.MENU_VOTING);
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
    }

    @Test
    @DisplayName("메뉴 후보는 노출 순서로 조회한다")
    void findMenuCandidates_returnsRepositoryResult() {
        UUID voteSessionId = UUID.randomUUID();
        VoteSession session = VoteSession.builder()
                .id(voteSessionId)
                .diningGroupId(UUID.randomUUID())
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
        given(voteSessionRepository.findById(voteSessionId)).willReturn(Optional.of(session));
        given(voteCandidateRepository.findAllByVoteSessionId(voteSessionId))
                .willReturn(List.of(candidate));
        MenuCandidateService service = new MenuCandidateService(
                voteSessionRepository,
                voteCandidateRepository
        );

        List<MenuVoteCandidate> result = service.findMenuCandidates(voteSessionId);

        assertThat(result).containsExactly(candidate);
    }
}
