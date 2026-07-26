package com.gm.core.domain.vote.candidate.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.gm.core.domain.group.model.Group;
import com.gm.core.domain.group.repository.GroupRepository;
import com.gm.core.domain.vote.candidate.model.MenuVoteCloseResult;
import com.gm.core.domain.vote.candidate.model.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.MenuVoteResult;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.core.domain.vote.candidate.repository.FinalMenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.MenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;
import com.gm.core.transaction.AfterCommitExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class MenuVotePostFinalizationPolicyTest {

    @Test
    @DisplayName("잔여 후보가 없으면 MENU_RECOMMENDING으로 즉시 전환한다")
    void noRemainingCandidate_recommendsAgain() {
        Fixture fixture = fixture();
        UUID candidateId = UUID.randomUUID();
        MenuVoteCount count = new MenuVoteCount(candidateId, 0, 0, 1, 1);
        MenuVoteResult rejected = new MenuVoteResult(count, VoteCandidateResult.REJECTED);
        fixture.prepare(List.of(count), List.of(rejected));
        given(fixture.voteSessionRepository.updateStatus(
                fixture.sessionId, VoteSessionStatus.MENU_RECOMMENDING))
                .willReturn(Optional.of(fixture.session(VoteSessionStatus.MENU_RECOMMENDING)));

        assertThat(fixture.service.finalizeVote(fixture.sessionId)).containsExactly(rejected);

        verify(fixture.voteSessionRepository).updateStatus(
                fixture.sessionId, VoteSessionStatus.MENU_RECOMMENDING);
        verifyNoInteractions(fixture.finalMenuVoteRepository);
    }

    @Test
    @DisplayName("잔여 후보가 둘이면 커밋 이후 ACTIVE 멤버 수로 Redis 최종 투표를 초기화한다")
    void twoRemainingCandidates_initializesFinalVoteAfterCommit() {
        Fixture fixture = fixture();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        MenuVoteCount firstCount = new MenuVoteCount(firstId, 2, 0, 0, 2);
        MenuVoteCount secondCount = new MenuVoteCount(secondId, 1, 1, 0, 2);
        List<MenuVoteResult> saved = List.of(
                new MenuVoteResult(firstCount, VoteCandidateResult.CONFIRMED),
                new MenuVoteResult(secondCount, VoteCandidateResult.KEPT));
        fixture.prepare(List.of(firstCount, secondCount), saved);
        given(fixture.voteSessionRepository.updateStatus(
                fixture.sessionId, VoteSessionStatus.MENU_SELECTION))
                .willReturn(Optional.of(fixture.session(VoteSessionStatus.MENU_SELECTION)));
        given(fixture.groupRepository.findById(fixture.groupId)).willReturn(Optional.of(new Group(
                fixture.groupId, UUID.randomUUID(), "점심", null, null, null,
                1000, null, 10, 4, null, null)));

        fixture.service.finalizeVote(fixture.sessionId);

        ArgumentCaptor<Runnable> callbacks = ArgumentCaptor.forClass(Runnable.class);
        verify(fixture.afterCommitExecutor, times(2)).execute(callbacks.capture());
        callbacks.getAllValues().forEach(Runnable::run);
        verify(fixture.finalMenuVoteRepository).initialize(
                fixture.sessionId, List.of(firstId, secondId), 4);
    }

    private Fixture fixture() {
        VoteSessionRepository sessionRepository = mock(VoteSessionRepository.class);
        VoteCandidateRepository candidateRepository = mock(VoteCandidateRepository.class);
        MenuVoteRepository menuVoteRepository = mock(MenuVoteRepository.class);
        GroupRepository groupRepository = mock(GroupRepository.class);
        MenuVoteResultPolicy resultPolicy = mock(MenuVoteResultPolicy.class);
        AfterCommitExecutor afterCommitExecutor = mock(AfterCommitExecutor.class);
        FinalMenuVoteRepository finalRepository = mock(FinalMenuVoteRepository.class);
        UUID sessionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        MenuVoteFinalizationService service = new MenuVoteFinalizationService(
                sessionRepository,
                candidateRepository,
                menuVoteRepository,
                groupRepository,
                resultPolicy,
                afterCommitExecutor,
                finalRepository
        );
        return new Fixture(
                sessionRepository,
                candidateRepository,
                menuVoteRepository,
                groupRepository,
                resultPolicy,
                afterCommitExecutor,
                finalRepository,
                service,
                sessionId,
                groupId
        );
    }

    private record Fixture(
            VoteSessionRepository voteSessionRepository,
            VoteCandidateRepository voteCandidateRepository,
            MenuVoteRepository menuVoteRepository,
            GroupRepository groupRepository,
            MenuVoteResultPolicy resultPolicy,
            AfterCommitExecutor afterCommitExecutor,
            FinalMenuVoteRepository finalMenuVoteRepository,
            MenuVoteFinalizationService service,
            UUID sessionId,
            UUID groupId
    ) {
        private void prepare(List<MenuVoteCount> snapshot, List<MenuVoteResult> saved) {
            given(voteSessionRepository.findByIdForUpdate(sessionId))
                    .willReturn(Optional.of(session(VoteSessionStatus.MENU_VOTING)));
            given(menuVoteRepository.closeAndGetSnapshot(sessionId))
                    .willReturn(MenuVoteCloseResult.success(snapshot));
            for (int index = 0; index < snapshot.size(); index++) {
                given(resultPolicy.decide(snapshot.get(index))).willReturn(saved.get(index).result());
            }
            given(voteCandidateRepository.saveMenuVoteResults(sessionId, saved)).willReturn(saved);
        }

        private VoteSession session(VoteSessionStatus status) {
            return VoteSession.builder()
                    .id(sessionId)
                    .diningGroupId(groupId)
                    .voteSessionStatus(status)
                    .title("점심 메뉴")
                    .build();
        }
    }
}
