package com.gm.core.domain.vote.candidate.service.state;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.candidate.model.finalmenu.FinalMenuVoteState;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteState;
import com.gm.core.domain.vote.candidate.repository.finalmenu.FinalMenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.menuvote.MenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.menu.VoteCandidateRepository;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VoteCurrentStateServiceTest {

    @Test
    @DisplayName("ACTIVE 그룹원에게 DB 세션·후보와 Redis 1차·최종투표 현재 상태를 함께 반환한다")
    void getState_combinesDatabaseAndRedisState() {
        GroupService groupService = mock(GroupService.class);
        VoteSessionRepository sessionRepository = mock(VoteSessionRepository.class);
        VoteCandidateRepository candidateRepository = mock(VoteCandidateRepository.class);
        MenuVoteRepository menuVoteRepository = mock(MenuVoteRepository.class);
        FinalMenuVoteRepository finalRepository = mock(FinalMenuVoteRepository.class);
        VoteCurrentStateService service = new VoteCurrentStateService(
                groupService, sessionRepository, candidateRepository,
                menuVoteRepository, finalRepository);
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        VoteSession session = VoteSession.builder()
                .id(sessionId)
                .diningGroupId(groupId)
                .voteSessionStatus(VoteSessionStatus.MENU_SELECTION)
                .title("점심")
                .build();
        MenuVoteState menuState = new MenuVoteState(
                MenuVoteState.Status.CLOSED, Instant.now(), List.of());
        FinalMenuVoteState finalState = new FinalMenuVoteState(
                FinalMenuVoteState.Status.OPEN, Instant.now(), List.of(), 0, null);
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(candidateRepository.findAllByVoteSessionId(sessionId)).willReturn(List.of());
        given(candidateRepository.findSelectedCandidate(sessionId)).willReturn(Optional.empty());
        given(menuVoteRepository.findState(sessionId)).willReturn(Optional.of(menuState));
        given(finalRepository.findState(sessionId)).willReturn(Optional.of(finalState));

        var result = service.getState(groupId, userId, sessionId);

        assertThat(result.sessionStatus()).isEqualTo(VoteSessionStatus.MENU_SELECTION);
        assertThat(result.menuVote()).contains(menuState);
        assertThat(result.finalMenuVote()).contains(finalState);
        verify(groupService).findGroupDetail(groupId, userId);
    }
}
