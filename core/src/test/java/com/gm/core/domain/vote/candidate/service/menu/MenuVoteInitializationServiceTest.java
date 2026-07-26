package com.gm.core.domain.vote.candidate.service.menu;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteSession;
import com.gm.core.domain.vote.candidate.model.menu.RecommendedMenuCandidate;
import com.gm.core.domain.vote.candidate.repository.menuvote.MenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.menu.VoteCandidateRepository;
import com.gm.core.domain.vote.candidate.service.menuvote.MenuVotePolicy;
import com.gm.core.domain.vote.session.service.VoteSessionService;
import com.gm.core.transaction.AfterCommitExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MenuVoteInitializationServiceTest {

    @Test
    @DisplayName("추천 DB 트랜잭션 커밋 뒤 Redis 메뉴 투표를 공통 30분 정책으로 연다")
    void completeRecommendation_initializesRedisAfterCommitForVotingDuration() {
        GroupService groupService = mock(GroupService.class);
        VoteSessionService voteSessionService = mock(VoteSessionService.class);
        VoteCandidateRepository candidateRepository = mock(VoteCandidateRepository.class);
        MenuVoteRepository menuVoteRepository = mock(MenuVoteRepository.class);
        CapturingAfterCommitExecutor afterCommitExecutor = new CapturingAfterCommitExecutor();
        UUID voteSessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        given(candidateRepository.saveNewCandidates(anyList()))
                .willAnswer(invocation -> {
                    List<com.gm.core.domain.vote.candidate.model.menu.VoteCandidate> candidates = invocation.getArgument(0);
                    return List.of(candidates.get(0).toBuilder().id(candidateId).build());
                });
        MenuCandidateService service = new MenuCandidateService(
                groupService,
                voteSessionService,
                candidateRepository,
                menuVoteRepository,
                afterCommitExecutor
        );

        service.completeRecommendation(voteSessionId, List.of(
                new RecommendedMenuCandidate(UUID.randomUUID(), 1, "추천")
        ));

        assertThat(afterCommitExecutor.action).isNotNull();
        verify(menuVoteRepository, org.mockito.Mockito.never())
                .initialize(org.mockito.ArgumentMatchers.any());
        afterCommitExecutor.action.run();
        verify(menuVoteRepository).initialize(new MenuVoteSession(
                voteSessionId,
                List.of(candidateId),
                MenuVotePolicy.VOTING_DURATION
        ));
    }

    private static final class CapturingAfterCommitExecutor extends AfterCommitExecutor {
        private Runnable action;

        @Override
        public void execute(Runnable action) {
            this.action = action;
        }
    }
}
