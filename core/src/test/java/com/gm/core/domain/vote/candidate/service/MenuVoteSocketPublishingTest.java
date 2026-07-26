package com.gm.core.domain.vote.candidate.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.candidate.model.MenuVoteChoice;
import com.gm.core.domain.vote.candidate.model.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.MenuVoteSubmission;
import com.gm.core.domain.vote.candidate.model.MenuVoteSubmitResult;
import com.gm.core.domain.vote.candidate.repository.MenuVoteRepository;
import com.gm.core.domain.vote.event.VoteEventType;
import com.gm.core.domain.vote.event.VoteSocketEventPublisher;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MenuVoteSocketPublishingTest {

    @Test
    @DisplayName("Redis 1차 투표 반영 성공 직후 최신 집계 이벤트를 발행한다")
    void submitVote_publishesUpdatedEvent() {
        GroupService groupService = mock(GroupService.class);
        VoteSessionRepository sessionRepository = mock(VoteSessionRepository.class);
        MenuVoteRepository voteRepository = mock(MenuVoteRepository.class);
        VoteSocketEventPublisher publisher = mock(VoteSocketEventPublisher.class);
        MenuVoteService service = new MenuVoteService(
                groupService, sessionRepository, voteRepository, publisher);
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        VoteSession session = VoteSession.builder()
                .id(sessionId)
                .diningGroupId(groupId)
                .voteSessionStatus(VoteSessionStatus.MENU_VOTING)
                .title("점심")
                .build();
        MenuVoteSubmission submission = new MenuVoteSubmission(
                MenuVoteChoice.GO,
                new MenuVoteCount(candidateId, 1, 0, 0, 1),
                true
        );
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(voteRepository.submit(sessionId, candidateId, userId, MenuVoteChoice.GO))
                .willReturn(MenuVoteSubmitResult.success(submission));

        service.submitVote(groupId, sessionId, candidateId, userId, MenuVoteChoice.GO);

        verify(publisher).publish(argThat(event ->
                event.eventType() == VoteEventType.MENU_VOTE_UPDATED
                        && event.voteSessionId().equals(sessionId)
                        && event.data().equals(submission)));
    }
}
