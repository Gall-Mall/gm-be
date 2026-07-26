package com.gm.core.domain.vote.candidate.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.candidate.model.MenuVoteSession;
import com.gm.core.domain.vote.candidate.model.RecommendedMenuCandidate;
import com.gm.core.domain.vote.candidate.repository.MenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.session.service.VoteSessionService;
import com.gm.core.transaction.AfterCommitExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MenuVoteInitializationServiceTest {

    @Test
    @DisplayName("추천 DB 트랜잭션 커밋 뒤 Redis 메뉴 투표를 정확히 1시간으로 연다")
    void completeRecommendation_initializesRedisAfterCommitForOneHour() {
        GroupService groupService = mock(GroupService.class);
        VoteSessionService voteSessionService = mock(VoteSessionService.class);
        VoteCandidateRepository candidateRepository = mock(VoteCandidateRepository.class);
        MenuVoteRepository menuVoteRepository = mock(MenuVoteRepository.class);
        CapturingAfterCommitExecutor afterCommitExecutor = new CapturingAfterCommitExecutor();
        UUID voteSessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        given(candidateRepository.saveNewCandidates(anyList()))
                .willAnswer(invocation -> {
                    List<com.gm.core.domain.vote.candidate.model.VoteCandidate> candidates = invocation.getArgument(0);
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
                Duration.ofHours(1)
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
