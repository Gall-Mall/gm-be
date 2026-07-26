package com.gm.core.domain.vote.session.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VoteSubscriptionAuthorizationServiceTest {

    private final GroupService groupService = mock(GroupService.class);
    private final VoteSessionRepository voteSessionRepository = mock(VoteSessionRepository.class);
    private final VoteSubscriptionAuthorizationService service =
            new VoteSubscriptionAuthorizationService(groupService, voteSessionRepository);

    @Test
    @DisplayName("세션 소속 그룹의 ACTIVE 멤버만 구독을 허용한다")
    void authorize_requiresActiveMemberOfSessionGroup() {
        UUID sessionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(voteSessionRepository.findById(sessionId)).willReturn(Optional.of(session(sessionId, groupId)));

        service.authorize(sessionId, userId);

        verify(groupService).findGroupDetail(groupId, userId);
    }

    @Test
    @DisplayName("존재하지 않는 세션 구독을 거부한다")
    void authorize_rejectsMissingSession() {
        UUID sessionId = UUID.randomUUID();
        given(voteSessionRepository.findById(sessionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.authorize(sessionId, UUID.randomUUID()))
                .isInstanceOf(VoteSessionException.class);
    }

    private VoteSession session(UUID sessionId, UUID groupId) {
        return VoteSession.builder()
                .id(sessionId)
                .diningGroupId(groupId)
                .voteSessionStatus(VoteSessionStatus.MENU_VOTING)
                .title("점심")
                .build();
    }
}
