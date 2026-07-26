package com.gm.db.domain.vote.candidate.repository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gm.core.domain.vote.candidate.exception.VoteCandidateException;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.db.domain.menu.category.repository.FoodCategoryJpaRepository;
import com.gm.db.domain.menu.menu.repository.MenuJpaRepository;
import com.gm.db.domain.vote.candidate.entity.VoteCandidateEntity;
import com.gm.db.domain.vote.candidate.mapper.MenuVoteCandidateMapper;
import com.gm.db.domain.vote.candidate.mapper.VoteCandidateMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VoteCandidateFinalSelectionRepositoryTest {

    @Test
    @DisplayName("세션 후보 행을 잠근 뒤 대상 하나만 selected=true로 저장하고 flush한다")
    void selectFinalCandidate_keepsExactlyOneSelected() {
        Fixture fixture = fixture();
        UUID sessionId = UUID.randomUUID();
        UUID selectedId = UUID.randomUUID();
        VoteCandidateEntity first = mock(VoteCandidateEntity.class);
        VoteCandidateEntity selected = mock(VoteCandidateEntity.class);
        given(first.getId()).willReturn(UUID.randomUUID());
        given(selected.getId()).willReturn(selectedId);
        given(selected.getResultStatus()).willReturn(VoteCandidateResult.CONFIRMED);
        given(fixture.jpaRepository.findAllByVoteSessionIdForUpdate(sessionId))
                .willReturn(List.of(first, selected));
        VoteCandidate expected = VoteCandidate.builder()
                .id(selectedId)
                .voteSessionId(sessionId)
                .menuId(UUID.randomUUID())
                .displayOrder(2)
                .selected(true)
                .build();
        given(fixture.mapper.toDomain(selected)).willReturn(expected);

        assertThat(fixture.repository.selectFinalCandidate(sessionId, selectedId))
                .isEqualTo(expected);

        var order = inOrder(first, selected, fixture.jpaRepository);
        order.verify(first).updateSelected(false);
        order.verify(selected).updateSelected(true);
        order.verify(fixture.jpaRepository).flush();
    }

    @Test
    @DisplayName("세션에 없는 후보는 최종 선택하지 않는다")
    void selectFinalCandidate_rejectsCandidateOutsideSession() {
        Fixture fixture = fixture();
        UUID sessionId = UUID.randomUUID();
        VoteCandidateEntity candidate = mock(VoteCandidateEntity.class);
        given(candidate.getId()).willReturn(UUID.randomUUID());
        given(fixture.jpaRepository.findAllByVoteSessionIdForUpdate(sessionId))
                .willReturn(List.of(candidate));

        assertThatThrownBy(() -> fixture.repository.selectFinalCandidate(
                sessionId, UUID.randomUUID()))
                .isInstanceOf(VoteCandidateException.class);

        verify(candidate, org.mockito.Mockito.never()).updateSelected(true);
    }

    private Fixture fixture() {
        VoteCandidateJpaRepository jpaRepository = mock(VoteCandidateJpaRepository.class);
        VoteCandidateMapper mapper = mock(VoteCandidateMapper.class);
        return new Fixture(
                jpaRepository,
                mapper,
                new VoteCandidateRepositoryImpl(
                        jpaRepository,
                        mock(MenuJpaRepository.class),
                        mock(FoodCategoryJpaRepository.class),
                        mapper,
                        mock(MenuVoteCandidateMapper.class)
                )
        );
    }

    private record Fixture(
            VoteCandidateJpaRepository jpaRepository,
            VoteCandidateMapper mapper,
            VoteCandidateRepositoryImpl repository
    ) {
    }
}
