package com.gm.core.domain.vote.candidate.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gm.core.domain.group.repository.GroupRepository;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.candidate.model.FinalMenuVoteResult;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.repository.FinalMenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.event.VoteSocketEventPublisher;
import com.gm.core.domain.vote.event.VoteEventType;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;
import com.gm.core.transaction.AfterCommitExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FinalMenuSelectionServiceTest {

    @Mock private VoteSessionRepository voteSessionRepository;
    @Mock private VoteCandidateRepository voteCandidateRepository;
    @Mock private FinalMenuVoteRepository finalMenuVoteRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupService groupService;
    @Mock private AfterCommitExecutor afterCommitExecutor;
    @Mock private VoteSocketEventPublisher voteSocketEventPublisher;

    @Test
    @DisplayName("두 후보 최종 투표는 전원 응답 전까지 Redis 결과만 반환한다")
    void submitFinalVote_beforeAllResponded_returnsWaiting() {
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID otherCandidateId = UUID.randomUUID();
        given(voteSessionRepository.findByIdForUpdate(sessionId))
                .willReturn(Optional.of(session(sessionId, groupId, VoteSessionStatus.MENU_SELECTION)));
        given(voteCandidateRepository.findRemainingCandidateIdsForUpdate(sessionId))
                .willReturn(List.of(candidateId, otherCandidateId));
        given(finalMenuVoteRepository.submit(sessionId, userId, candidateId))
                .willReturn(FinalMenuVoteResult.waiting());

        FinalMenuVoteResult result = service().submitFinalVote(
                groupId, userId, sessionId, candidateId);

        assertThat(result.status()).isEqualTo(FinalMenuVoteResult.Status.WAITING);
        verify(groupService).findGroupDetail(groupId, userId);
    }

    @Test
    @DisplayName("두 후보 최종 투표의 단독 1위는 DB 최종 메뉴와 다음 상태를 원자 반영한다")
    void submitFinalVote_whenAllResponded_selectsWinner() {
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        UUID otherCandidateId = UUID.randomUUID();
        VoteSession menuSelection = session(sessionId, groupId, VoteSessionStatus.MENU_SELECTION);
        given(voteSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.of(menuSelection));
        given(voteCandidateRepository.findRemainingCandidateIdsForUpdate(sessionId))
                .willReturn(List.of(winnerId, otherCandidateId));
        given(finalMenuVoteRepository.submit(sessionId, userId, winnerId))
                .willReturn(FinalMenuVoteResult.selected(winnerId));
        given(voteCandidateRepository.selectFinalCandidate(sessionId, winnerId))
                .willReturn(candidate(winnerId, sessionId));
        given(voteSessionRepository.updateStatus(sessionId, VoteSessionStatus.RESTAURANT_SEARCHING))
                .willReturn(Optional.of(session(
                        sessionId, groupId, VoteSessionStatus.RESTAURANT_SEARCHING)));

        FinalMenuVoteResult result = service().submitFinalVote(
                groupId, userId, sessionId, winnerId);

        assertThat(result.selectedCandidateId()).isEqualTo(winnerId);
        verify(voteCandidateRepository).selectFinalCandidate(sessionId, winnerId);
        verify(voteSessionRepository).updateStatus(
                sessionId, VoteSessionStatus.RESTAURANT_SEARCHING);
        verify(finalMenuVoteRepository, never()).findState(sessionId);
        verifyNoInteractions(voteSocketEventPublisher);
        ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
        verify(afterCommitExecutor).execute(callback.capture());
        callback.getValue().run();
        verify(voteSocketEventPublisher).publish(argThat(event ->
                event.eventType() == VoteEventType.FINAL_MENU_SELECTED
                        && event.voteSessionId().equals(sessionId)));
    }

    @Test
    @DisplayName("세 후보 이상이면 활성 방장이 최종 메뉴를 직접 선택한다")
    void selectByOwner_withThreeCandidates_selectsCandidate() {
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID selectedId = UUID.randomUUID();
        VoteSession menuSelection = session(sessionId, groupId, VoteSessionStatus.MENU_SELECTION);
        given(groupRepository.isActiveOwner(groupId, ownerId)).willReturn(true);
        given(voteSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.of(menuSelection));
        given(voteCandidateRepository.findRemainingCandidateIdsForUpdate(sessionId))
                .willReturn(List.of(UUID.randomUUID(), selectedId, UUID.randomUUID()));
        VoteCandidate selected = candidate(selectedId, sessionId);
        given(voteCandidateRepository.selectFinalCandidate(sessionId, selectedId))
                .willReturn(selected);
        given(voteSessionRepository.updateStatus(sessionId, VoteSessionStatus.RESTAURANT_SEARCHING))
                .willReturn(Optional.of(session(
                        sessionId, groupId, VoteSessionStatus.RESTAURANT_SEARCHING)));

        assertThat(service().selectByOwner(groupId, ownerId, sessionId, selectedId))
                .isEqualTo(selected);
    }

    @Test
    @DisplayName("후보 하나에서 방장이 재추천하면 MENU_RECOMMENDING으로 전환한다")
    void reRecommendSingleCandidate_changesStatus() {
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        given(groupRepository.isActiveOwner(groupId, ownerId)).willReturn(true);
        given(voteSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.of(
                session(sessionId, groupId, VoteSessionStatus.MENU_SELECTION)));
        given(voteCandidateRepository.findRemainingCandidateIdsForUpdate(sessionId))
                .willReturn(List.of(UUID.randomUUID()));
        given(voteSessionRepository.updateStatus(sessionId, VoteSessionStatus.MENU_RECOMMENDING))
                .willReturn(Optional.of(session(
                        sessionId, groupId, VoteSessionStatus.MENU_RECOMMENDING)));

        service().reRecommendSingleCandidate(groupId, ownerId, sessionId);

        verify(voteSessionRepository).updateStatus(
                sessionId, VoteSessionStatus.MENU_RECOMMENDING);
    }

    @Test
    @DisplayName("이미 최종 메뉴가 확정된 세션의 자동 만료 재처리는 DB를 다시 선택하지 않는다")
    void selectExpiredWinner_alreadySelected_onlyRetriesRedisCleanup() {
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        given(voteSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.of(
                session(sessionId, UUID.randomUUID(), VoteSessionStatus.RESTAURANT_SEARCHING)));

        service().selectExpiredWinner(sessionId, candidateId);

        verify(voteCandidateRepository, never()).selectFinalCandidate(sessionId, candidateId);
        verify(afterCommitExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));
    }

    private FinalMenuSelectionService service() {
        return new FinalMenuSelectionService(
                voteSessionRepository,
                voteCandidateRepository,
                finalMenuVoteRepository,
                groupRepository,
                groupService,
                afterCommitExecutor,
                voteSocketEventPublisher
        );
    }

    private VoteSession session(UUID id, UUID groupId, VoteSessionStatus status) {
        return VoteSession.builder()
                .id(id)
                .diningGroupId(groupId)
                .voteSessionStatus(status)
                .title("점심 메뉴")
                .build();
    }

    private VoteCandidate candidate(UUID id, UUID sessionId) {
        return VoteCandidate.builder()
                .id(id)
                .voteSessionId(sessionId)
                .menuId(UUID.randomUUID())
                .displayOrder(1)
                .selected(true)
                .build();
    }
}
