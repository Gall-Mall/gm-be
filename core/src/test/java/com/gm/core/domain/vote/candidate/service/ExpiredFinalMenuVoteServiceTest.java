package com.gm.core.domain.vote.candidate.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gm.core.domain.vote.candidate.model.FinalMenuVoteCloseResult;
import com.gm.core.domain.vote.candidate.repository.FinalMenuVoteRepository;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExpiredFinalMenuVoteServiceTest {

    private final FinalMenuVoteRepository repository = mock(FinalMenuVoteRepository.class);
    private final FinalMenuSelectionService selectionService = mock(FinalMenuSelectionService.class);
    private final ExpiredFinalMenuVoteService service =
            new ExpiredFinalMenuVoteService(repository, selectionService);

    @Test
    @DisplayName("만료된 최종투표의 단독 1위를 기존 DB 선택 서비스로 확정한다")
    void finalizeExpired_uniqueWinner_selectsCandidate() {
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        Instant now = Instant.now();
        given(repository.findExpired(now, 100)).willReturn(List.of(sessionId));
        given(repository.closeExpired(sessionId))
                .willReturn(FinalMenuVoteCloseResult.uniqueWinner(candidateId));

        service.finalizeExpired(now);

        verify(selectionService).selectExpiredWinner(sessionId, candidateId);
    }

    @Test
    @DisplayName("0응답 또는 동점 마감은 방장 선택 대기를 유지하고 만료 인덱스를 정리한다")
    void finalizeExpired_ownerSelectionPending_removesExpiration() {
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        given(repository.findExpired(now, 100)).willReturn(List.of(sessionId));
        given(repository.closeExpired(sessionId))
                .willReturn(FinalMenuVoteCloseResult.ownerSelectionPending());

        service.finalizeExpired(now);

        verify(repository).removeExpiration(sessionId);
    }

    @Test
    @DisplayName("한 세션 마감이 실패해도 다음 만료 세션을 계속 처리한다")
    void finalizeExpired_whenOneSessionFails_continuesNextSession() {
        UUID failedSessionId = UUID.randomUUID();
        UUID nextSessionId = UUID.randomUUID();
        Instant now = Instant.now();
        given(repository.findExpired(now, 100))
                .willReturn(List.of(failedSessionId, nextSessionId));
        given(repository.closeExpired(failedSessionId))
                .willThrow(new IllegalStateException("broken state"));
        given(repository.closeExpired(nextSessionId))
                .willReturn(FinalMenuVoteCloseResult.ownerSelectionPending());

        service.finalizeExpired(now);

        verify(repository).closeExpired(nextSessionId);
        verify(repository).removeExpiration(nextSessionId);
    }
}
