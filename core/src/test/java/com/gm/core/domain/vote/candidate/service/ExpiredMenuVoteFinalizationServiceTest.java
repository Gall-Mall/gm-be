package com.gm.core.domain.vote.candidate.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gm.core.domain.vote.session.service.VoteSessionService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpiredMenuVoteFinalizationServiceTest {

    @Mock private VoteSessionService voteSessionService;
    @Mock private MenuVoteFinalizationService menuVoteFinalizationService;

    @Test
    @DisplayName("DB startedAt 기준 1시간 만료 세션을 오래된 순서로 제한 마감한다")
    void finalizeExpiredVotes_closesDatabaseSelectedBatch() {
        UUID oldest = UUID.randomUUID();
        UUID next = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 7, 25, 12, 0);
        given(voteSessionService.findExpiredMenuVoting(
                now.minus(MenuVotePolicy.VOTING_DURATION),
                100
        ))
                .willReturn(List.of(oldest, next));

        service().finalizeExpiredVotes(now);

        InOrder inOrder = inOrder(menuVoteFinalizationService);
        inOrder.verify(menuVoteFinalizationService).finalizeVote(oldest);
        inOrder.verify(menuVoteFinalizationService).finalizeVote(next);
    }

    @Test
    @DisplayName("한 세션 마감 실패가 같은 배치의 다른 만료 세션 처리를 막지 않는다")
    void finalizeExpiredVotes_continuesAfterOneSessionFailure() {
        UUID failed = UUID.randomUUID();
        UUID healthy = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 7, 25, 12, 0);
        given(voteSessionService.findExpiredMenuVoting(
                now.minus(MenuVotePolicy.VOTING_DURATION),
                100
        ))
                .willReturn(List.of(failed, healthy));
        doThrow(new IllegalStateException("temporary failure"))
                .when(menuVoteFinalizationService).finalizeVote(failed);

        service().finalizeExpiredVotes(now);

        verify(menuVoteFinalizationService).finalizeVote(healthy);
    }

    private ExpiredMenuVoteFinalizationService service() {
        return new ExpiredMenuVoteFinalizationService(voteSessionService, menuVoteFinalizationService);
    }
}
