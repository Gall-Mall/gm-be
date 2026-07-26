package com.gm.api.controller.vote.candidate;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.vote.candidate.model.FinalMenuVoteResult;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.service.FinalMenuSelectionService;
import com.gm.core.domain.vote.candidate.service.MenuCandidateService;
import com.gm.core.domain.vote.candidate.service.MenuVoteFinalizationService;
import com.gm.core.domain.vote.candidate.service.MenuVoteService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FinalMenuSelectionControllerTest {

    @Test
    @DisplayName("활성 사용자 식별자로 두 후보 최종 투표를 제출한다")
    void submitFinalMenuVote_delegatesAuthenticatedUser() {
        Fixture fixture = fixture();
        UUID candidateId = UUID.randomUUID();
        given(fixture.service.submitFinalVote(
                fixture.groupId, fixture.userId, fixture.sessionId, candidateId))
                .willReturn(FinalMenuVoteResult.waiting());

        fixture.controller.submitFinalMenuVote(
                fixture.groupId, fixture.sessionId, candidateId, fixture.principal);

        verify(fixture.service).submitFinalVote(
                fixture.groupId, fixture.userId, fixture.sessionId, candidateId);
    }

    @Test
    @DisplayName("방장 최종 선택 API는 인증 사용자와 경로 소속을 서비스에 전달한다")
    void selectFinalMenu_delegatesAuthenticatedOwner() {
        Fixture fixture = fixture();
        UUID candidateId = UUID.randomUUID();
        given(fixture.service.selectByOwner(
                fixture.groupId, fixture.userId, fixture.sessionId, candidateId))
                .willReturn(VoteCandidate.builder()
                        .id(candidateId)
                        .voteSessionId(fixture.sessionId)
                        .menuId(UUID.randomUUID())
                        .displayOrder(1)
                        .selected(true)
                        .build());

        fixture.controller.selectFinalMenu(
                fixture.groupId, fixture.sessionId, candidateId, fixture.principal);

        verify(fixture.service).selectByOwner(
                fixture.groupId, fixture.userId, fixture.sessionId, candidateId);
    }

    @Test
    @DisplayName("한 후보 재추천 API는 인증된 방장 식별자를 서비스에 전달한다")
    void reRecommendSingleCandidate_delegatesAuthenticatedOwner() {
        Fixture fixture = fixture();

        fixture.controller.reRecommendSingleCandidate(
                fixture.groupId, fixture.sessionId, fixture.principal);

        verify(fixture.service).reRecommendSingleCandidate(
                fixture.groupId, fixture.userId, fixture.sessionId);
    }

    private Fixture fixture() {
        MenuCandidateService candidateService = mock(MenuCandidateService.class);
        MenuVoteService voteService = mock(MenuVoteService.class);
        MenuVoteFinalizationService finalizationService = mock(MenuVoteFinalizationService.class);
        FinalMenuSelectionService selectionService = mock(FinalMenuSelectionService.class);
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
        given(principal.getUserId()).willReturn(userId);
        return new Fixture(
                new MenuCandidateController(
                        candidateService,
                        voteService,
                        finalizationService,
                        selectionService
                ),
                selectionService,
                principal,
                groupId,
                sessionId,
                userId
        );
    }

    private record Fixture(
            MenuCandidateController controller,
            FinalMenuSelectionService service,
            CustomUserPrincipal principal,
            UUID groupId,
            UUID sessionId,
            UUID userId
    ) {
    }
}
