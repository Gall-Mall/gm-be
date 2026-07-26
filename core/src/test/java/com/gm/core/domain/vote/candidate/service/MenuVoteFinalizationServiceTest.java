package com.gm.core.domain.vote.candidate.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.repository.GroupRepository;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateErrorCode;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateException;
import com.gm.core.domain.vote.candidate.model.MenuVoteCloseResult;
import com.gm.core.domain.vote.candidate.model.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.MenuVoteResult;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.core.domain.vote.candidate.repository.MenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.FinalMenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.event.VoteEventType;
import com.gm.core.domain.vote.event.VoteSocketEventPublisher;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;
import com.gm.core.transaction.AfterCommitExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MenuVoteFinalizationServiceTest {

    @Mock private VoteSessionRepository voteSessionRepository;
    @Mock private VoteCandidateRepository voteCandidateRepository;
    @Mock private MenuVoteRepository menuVoteRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private AfterCommitExecutor afterCommitExecutor;
    @Mock private FinalMenuVoteRepository finalMenuVoteRepository;
    @Mock private VoteSocketEventPublisher voteSocketEventPublisher;

    @Test
    @DisplayName("Redis 고정 스냅샷을 판정해 DB에 저장하고 MENU_SELECTION으로 전환한다")
    void finalizeVote_closesSnapshotAndPersistsDecisions() {
        UUID voteSessionId = UUID.randomUUID();
        MenuVoteCount confirmed = new MenuVoteCount(UUID.randomUUID(), 2, 0, 0, 2);
        MenuVoteCount rejected = new MenuVoteCount(UUID.randomUUID(), 0, 0, 1, 1);
        List<MenuVoteResult> expected = List.of(
                new MenuVoteResult(confirmed, VoteCandidateResult.CONFIRMED),
                new MenuVoteResult(rejected, VoteCandidateResult.REJECTED)
        );
        given(voteSessionRepository.findByIdForUpdate(voteSessionId))
                .willReturn(Optional.of(session(voteSessionId, VoteSessionStatus.MENU_VOTING)));
        given(menuVoteRepository.closeAndGetSnapshot(voteSessionId))
                .willReturn(MenuVoteCloseResult.success(List.of(confirmed, rejected)));
        given(voteCandidateRepository.saveMenuVoteResults(voteSessionId, expected))
                .willReturn(expected);
        given(voteSessionRepository.updateStatus(voteSessionId, VoteSessionStatus.MENU_SELECTION))
                .willReturn(Optional.of(session(voteSessionId, VoteSessionStatus.MENU_SELECTION)));
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(afterCommitExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));

        assertThat(service().finalizeVote(voteSessionId)).isEqualTo(expected);

        InOrder order = inOrder(
                menuVoteRepository,
                voteCandidateRepository,
                voteSessionRepository,
                afterCommitExecutor
        );
        order.verify(menuVoteRepository).closeAndGetSnapshot(voteSessionId);
        order.verify(voteCandidateRepository).saveMenuVoteResults(voteSessionId, expected);
        order.verify(voteSessionRepository).updateStatus(voteSessionId, VoteSessionStatus.MENU_SELECTION);
        order.verify(afterCommitExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));
        verify(voteSocketEventPublisher).publish(argThat(event ->
                event.eventType() == VoteEventType.MENU_VOTE_CLOSED
                        && event.voteSessionId().equals(voteSessionId)));
    }

    @Test
    @DisplayName("DB 저장 실패 시 닫힌 Redis 스냅샷을 삭제하지 않는다")
    void finalizeVote_whenDatabaseFails_retainsClosedSnapshot() {
        UUID voteSessionId = UUID.randomUUID();
        MenuVoteCount count = new MenuVoteCount(UUID.randomUUID(), 1, 0, 0, 1);
        given(voteSessionRepository.findByIdForUpdate(voteSessionId))
                .willReturn(Optional.of(session(voteSessionId, VoteSessionStatus.MENU_VOTING)));
        given(menuVoteRepository.closeAndGetSnapshot(voteSessionId))
                .willReturn(MenuVoteCloseResult.success(List.of(count)));
        doThrow(new IllegalStateException("db failure"))
                .when(voteCandidateRepository)
                .saveMenuVoteResults(
                        org.mockito.ArgumentMatchers.eq(voteSessionId),
                        org.mockito.ArgumentMatchers.anyList()
                );

        assertThatThrownBy(() -> service().finalizeVote(voteSessionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db failure");

        verifyNoInteractions(afterCommitExecutor);
    }

    @Test
    @DisplayName("Redis 마감 스냅샷이 없으면 세션을 FAILED로 전환한다")
    void finalizeVote_whenSnapshotIsMissing_marksSessionAsFailed() {
        UUID voteSessionId = UUID.randomUUID();
        given(voteSessionRepository.findByIdForUpdate(voteSessionId))
                .willReturn(Optional.of(session(voteSessionId, VoteSessionStatus.MENU_VOTING)));
        given(menuVoteRepository.closeAndGetSnapshot(voteSessionId))
                .willReturn(MenuVoteCloseResult.snapshotNotFound());
        given(voteSessionRepository.updateStatus(voteSessionId, VoteSessionStatus.FAILED))
                .willReturn(Optional.of(session(voteSessionId, VoteSessionStatus.FAILED)));

        assertThatThrownBy(() -> service().finalizeVote(voteSessionId))
                .isInstanceOfSatisfying(VoteCandidateException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(VoteCandidateErrorCode.VOTE_SNAPSHOT_NOT_FOUND));

        verify(voteSessionRepository).updateStatus(voteSessionId, VoteSessionStatus.FAILED);
        verify(voteCandidateRepository, never()).saveMenuVoteResults(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList()
        );
        verifyNoInteractions(afterCommitExecutor);
    }

    @Test
    @DisplayName("이미 저장된 세션은 결과를 중복 저장하지 않고 Redis 정리만 다시 시도한다")
    void finalizeVote_whenAlreadyFinalized_returnsStoredResultsAndRetriesCleanup() {
        UUID voteSessionId = UUID.randomUUID();
        List<MenuVoteResult> stored = List.of(new MenuVoteResult(
                new MenuVoteCount(UUID.randomUUID(), 1, 0, 0, 1),
                VoteCandidateResult.CONFIRMED
        ));
        given(voteSessionRepository.findByIdForUpdate(voteSessionId))
                .willReturn(Optional.of(session(voteSessionId, VoteSessionStatus.MENU_SELECTION)));
        given(voteCandidateRepository.findMenuVoteResults(voteSessionId)).willReturn(stored);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(afterCommitExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));

        assertThat(service().finalizeVote(voteSessionId)).isEqualTo(stored);

        verify(menuVoteRepository, never()).closeAndGetSnapshot(voteSessionId);
        verify(voteCandidateRepository, never()).saveMenuVoteResults(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList()
        );
        verify(menuVoteRepository).delete(voteSessionId);
    }

    @Test
    @DisplayName("수동 마감은 활성 방장과 한 명 이상의 응답을 요구한다")
    void finalizeVoteManually_requiresOwnerAndResponse() {
        UUID groupId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        given(voteSessionRepository.findByIdForUpdate(voteSessionId))
                .willReturn(Optional.of(session(voteSessionId, groupId, VoteSessionStatus.MENU_VOTING)));
        given(groupRepository.isActiveOwner(groupId, requesterUserId)).willReturn(true);
        given(menuVoteRepository.closeAndGetSnapshotIfAnyResponse(voteSessionId))
                .willReturn(MenuVoteCloseResult.noResponse());

        assertThatThrownBy(() -> service().finalizeVoteManually(
                groupId,
                requesterUserId,
                voteSessionId
        )).isInstanceOf(VoteCandidateException.class);

        verify(voteCandidateRepository, never()).saveMenuVoteResults(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    @DisplayName("활성 방장이 아니면 수동 마감을 거부한다")
    void finalizeVoteManually_rejectsNonOwner() {
        UUID groupId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        given(voteSessionRepository.findByIdForUpdate(voteSessionId))
                .willReturn(Optional.of(session(voteSessionId, groupId, VoteSessionStatus.MENU_VOTING)));

        assertThatThrownBy(() -> service().finalizeVoteManually(
                groupId,
                requesterUserId,
                voteSessionId
        )).isInstanceOf(GroupException.class);

        verifyNoInteractions(menuVoteRepository, voteCandidateRepository);
    }

    private MenuVoteFinalizationService service() {
        return new MenuVoteFinalizationService(
                voteSessionRepository,
                voteCandidateRepository,
                menuVoteRepository,
                groupRepository,
                new MenuVoteResultPolicy(),
                afterCommitExecutor,
                finalMenuVoteRepository,
                voteSocketEventPublisher
        );
    }

    private VoteSession session(UUID id, VoteSessionStatus status) {
        return session(id, UUID.randomUUID(), status);
    }

    private VoteSession session(UUID id, UUID groupId, VoteSessionStatus status) {
        return VoteSession.builder()
                .id(id)
                .diningGroupId(groupId)
                .voteSessionStatus(status)
                .title("저녁 메뉴 투표")
                .build();
    }
}
