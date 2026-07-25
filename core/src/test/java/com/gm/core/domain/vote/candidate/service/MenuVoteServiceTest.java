package com.gm.core.domain.vote.candidate.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gm.core.domain.group.exception.GroupErrorCode;
import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateErrorCode;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateException;
import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.MenuVoteChoice;
import com.gm.core.domain.vote.candidate.model.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.MenuVoteSubmission;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.core.domain.vote.candidate.repository.MenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MenuVoteServiceTest {

    @Mock
    private GroupService groupService;

    @Mock
    private VoteSessionRepository voteSessionRepository;

    @Mock
    private VoteCandidateRepository voteCandidateRepository;

    @Mock
    private MenuVoteRepository menuVoteRepository;

    @Test
    @DisplayName("ACTIVE 멤버가 메뉴 투표 중인 세션 후보에 선택을 반영한다")
    void submitVote_submitsChoiceForCandidate() {
        UUID groupId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(voteSessionRepository.findById(voteSessionId))
                .willReturn(Optional.of(session(voteSessionId, groupId, VoteSessionStatus.MENU_VOTING)));
        given(voteCandidateRepository.findAllByVoteSessionId(voteSessionId))
                .willReturn(List.of(candidate(candidateId, voteSessionId)));
        MenuVoteSubmission expected = new MenuVoteSubmission(
                MenuVoteChoice.GO,
                new MenuVoteCount(candidateId, 1, 0, 0, 1),
                true
        );
        given(menuVoteRepository.submit(voteSessionId, candidateId, userId, MenuVoteChoice.GO))
                .willReturn(expected);

        MenuVoteSubmission result = service().submitVote(
                groupId,
                voteSessionId,
                candidateId,
                userId,
                MenuVoteChoice.GO
        );

        assertThat(result).isEqualTo(expected);
        verify(groupService).findGroupDetail(groupId, userId);
        verify(menuVoteRepository).submit(voteSessionId, candidateId, userId, MenuVoteChoice.GO);
    }

    @Test
    @DisplayName("ACTIVE 그룹 멤버가 아니면 투표 저장소를 호출하지 않는다")
    void submitVote_rejectsInactiveGroupMember() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(groupService.findGroupDetail(groupId, userId))
                .willThrow(new GroupException(GroupErrorCode.NOT_GROUP_MEMBER));

        assertThatThrownBy(() -> service().submitVote(
                groupId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                MenuVoteChoice.NO
        )).isInstanceOfSatisfying(GroupException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER));
        verifyNoInteractions(voteSessionRepository, voteCandidateRepository, menuVoteRepository);
    }

    @Test
    @DisplayName("경로 그룹과 세션 그룹이 다르면 투표를 거부한다")
    void submitVote_rejectsSessionOutsideRouteGroup() {
        UUID groupId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(voteSessionRepository.findById(voteSessionId))
                .willReturn(Optional.of(session(
                        voteSessionId,
                        UUID.randomUUID(),
                        VoteSessionStatus.MENU_VOTING
                )));

        assertThatThrownBy(() -> service().submitVote(
                groupId,
                voteSessionId,
                UUID.randomUUID(),
                userId,
                MenuVoteChoice.GO
        )).isInstanceOfSatisfying(VoteSessionException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(VoteSessionErrorCode.SESSION_NOT_FOUND));
        verifyNoInteractions(voteCandidateRepository, menuVoteRepository);
    }

    @Test
    @DisplayName("메뉴 투표 단계가 아니면 Redis에 선택을 전달하지 않는다")
    void submitVote_rejectsSessionOutsideMenuVoting() {
        UUID groupId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        given(voteSessionRepository.findById(voteSessionId))
                .willReturn(Optional.of(session(voteSessionId, groupId, VoteSessionStatus.MENU_SELECTION)));

        assertThatThrownBy(() -> service().submitVote(
                groupId,
                voteSessionId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                MenuVoteChoice.GO
        )).isInstanceOfSatisfying(VoteCandidateException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(VoteCandidateErrorCode.VOTE_ALREADY_CLOSED));
        verifyNoInteractions(voteCandidateRepository, menuVoteRepository);
    }

    @Test
    @DisplayName("다른 세션에 속한 후보에는 투표할 수 없다")
    void submitVote_rejectsCandidateOutsideSession() {
        UUID groupId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        given(voteSessionRepository.findById(voteSessionId))
                .willReturn(Optional.of(session(voteSessionId, groupId, VoteSessionStatus.MENU_VOTING)));
        given(voteCandidateRepository.findAllByVoteSessionId(voteSessionId))
                .willReturn(List.of(candidate(UUID.randomUUID(), voteSessionId)));

        assertThatThrownBy(() -> service().submitVote(
                groupId,
                voteSessionId,
                candidateId,
                UUID.randomUUID(),
                MenuVoteChoice.NO
        )).isInstanceOfSatisfying(VoteCandidateException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(VoteCandidateErrorCode.CANDIDATE_NOT_FOUND));
        verifyNoInteractions(menuVoteRepository);
    }

    private MenuVoteService service() {
        return new MenuVoteService(
                groupService,
                voteSessionRepository,
                voteCandidateRepository,
                menuVoteRepository
        );
    }

    private VoteSession session(UUID id, UUID groupId, VoteSessionStatus status) {
        return VoteSession.builder()
                .id(id)
                .diningGroupId(groupId)
                .voteSessionStatus(status)
                .title("저녁 메뉴 투표")
                .build();
    }

    private MenuVoteCandidate candidate(UUID candidateId, UUID voteSessionId) {
        return new MenuVoteCandidate(
                candidateId,
                voteSessionId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "김치찌개",
                "한식",
                null,
                1,
                0,
                0,
                0,
                0,
                VoteCandidateResult.PENDING,
                "따뜻한 국물 메뉴"
        );
    }
}
