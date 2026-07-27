package com.gm.db.domain.vote.candidate.repository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteResult;
import com.gm.core.domain.vote.candidate.model.menu.VoteCandidate;
import com.gm.core.domain.vote.candidate.model.menu.VoteCandidateResult;
import com.gm.db.domain.menu.category.repository.FoodCategoryJpaRepository;
import com.gm.db.domain.menu.menu.repository.MenuJpaRepository;
import com.gm.db.domain.vote.candidate.entity.VoteCandidateEntity;
import com.gm.db.domain.vote.candidate.mapper.MenuVoteCandidateMapper;
import com.gm.db.domain.vote.candidate.mapper.VoteCandidateMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class VoteCandidateRepositoryImplTest {

    @Test
    @DisplayName("새 추천 후보를 저장할 때 같은 세션의 이전 라운드 후보를 교체한다")
    void saveNewCandidates_replacesPreviousRoundCandidates() {
        VoteCandidateJpaRepository jpaRepository = mock(VoteCandidateJpaRepository.class);
        VoteCandidateMapper mapper = mock(VoteCandidateMapper.class);
        UUID voteSessionId = UUID.randomUUID();
        VoteCandidateEntity previous = mock(VoteCandidateEntity.class);
        VoteCandidateEntity replacement = mock(VoteCandidateEntity.class);
        VoteCandidate candidate = VoteCandidate.builder()
                .voteSessionId(voteSessionId)
                .menuId(UUID.randomUUID())
                .displayOrder(1)
                .build();
        given(jpaRepository.findAllByVoteSessionIdOrderByDisplayOrderAsc(voteSessionId))
                .willReturn(List.of(previous));
        given(mapper.toEntity(candidate)).willReturn(replacement);
        given(jpaRepository.saveAll(List.of(replacement))).willReturn(List.of(replacement));
        given(mapper.toDomain(replacement)).willReturn(candidate);
        VoteCandidateRepositoryImpl repository = new VoteCandidateRepositoryImpl(
                jpaRepository,
                mock(MenuJpaRepository.class),
                mock(FoodCategoryJpaRepository.class),
                mapper,
                mock(MenuVoteCandidateMapper.class)
        );

        assertThat(repository.saveNewCandidates(List.of(candidate))).containsExactly(candidate);

        var ordered = inOrder(jpaRepository);
        ordered.verify(jpaRepository)
                .findAllByVoteSessionIdOrderByDisplayOrderAsc(voteSessionId);
        ordered.verify(jpaRepository).deleteAllInBatch(List.of(previous));
        ordered.verify(jpaRepository).flush();
        ordered.verify(jpaRepository).saveAll(List.of(replacement));
    }

    @Test
    @DisplayName("세션의 모든 후보에 최종 집계와 판정을 반영한 뒤 flush한다")
    void saveMenuVoteResults_updatesAllCandidatesAndFlushes() {
        VoteCandidateJpaRepository jpaRepository = mock(VoteCandidateJpaRepository.class);
        VoteCandidateEntity first = mock(VoteCandidateEntity.class);
        VoteCandidateEntity second = mock(VoteCandidateEntity.class);
        UUID voteSessionId = UUID.randomUUID();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        given(first.getId()).willReturn(firstId);
        given(second.getId()).willReturn(secondId);
        given(jpaRepository.findAllByVoteSessionIdOrderByDisplayOrderAsc(voteSessionId))
                .willReturn(List.of(first, second));
        MenuVoteResult firstResult = new MenuVoteResult(
                new MenuVoteCount(firstId, 2, 1, 0, 3),
                VoteCandidateResult.CONFIRMED
        );
        MenuVoteResult secondResult = new MenuVoteResult(
                new MenuVoteCount(secondId, 0, 0, 2, 2),
                VoteCandidateResult.REJECTED
        );

        repository(jpaRepository).saveMenuVoteResults(
                voteSessionId,
                List.of(firstResult, secondResult)
        );

        verify(first).finalizeMenuVote(firstResult.count(), firstResult.result());
        verify(second).finalizeMenuVote(secondResult.count(), secondResult.result());
        verify(jpaRepository).flush();
    }

    @Test
    @DisplayName("최종 결과는 Redis 스냅샷 순서와 관계없이 후보 노출 순서로 반환한다")
    void saveMenuVoteResults_returnsResultsInDisplayOrder() {
        VoteCandidateJpaRepository jpaRepository = mock(VoteCandidateJpaRepository.class);
        VoteCandidateEntity first = mock(VoteCandidateEntity.class);
        VoteCandidateEntity second = mock(VoteCandidateEntity.class);
        UUID voteSessionId = UUID.randomUUID();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        given(first.getId()).willReturn(firstId);
        given(second.getId()).willReturn(secondId);
        given(jpaRepository.findAllByVoteSessionIdOrderByDisplayOrderAsc(voteSessionId))
                .willReturn(List.of(first, second));
        MenuVoteResult firstResult = new MenuVoteResult(
                new MenuVoteCount(firstId, 2, 0, 0, 2),
                VoteCandidateResult.CONFIRMED
        );
        MenuVoteResult secondResult = new MenuVoteResult(
                new MenuVoteCount(secondId, 0, 1, 0, 1),
                VoteCandidateResult.KEPT
        );

        List<MenuVoteResult> saved = repository(jpaRepository).saveMenuVoteResults(
                voteSessionId,
                List.of(secondResult, firstResult)
        );

        assertThat(saved).containsExactly(firstResult, secondResult);
    }

    @Test
    @DisplayName("Redis 스냅샷 후보 집합이 DB와 다르면 어떤 결과도 flush하지 않는다")
    void saveMenuVoteResults_rejectsMismatchedSnapshot() {
        VoteCandidateJpaRepository jpaRepository = mock(VoteCandidateJpaRepository.class);
        VoteCandidateEntity candidate = mock(VoteCandidateEntity.class);
        UUID voteSessionId = UUID.randomUUID();
        given(jpaRepository.findAllByVoteSessionIdOrderByDisplayOrderAsc(voteSessionId))
                .willReturn(List.of(candidate));

        assertThatThrownBy(() -> repository(jpaRepository).saveMenuVoteResults(
                voteSessionId,
                List.of()
        )).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(candidate);
    }


    private VoteCandidateRepositoryImpl repository(VoteCandidateJpaRepository jpaRepository) {
        return new VoteCandidateRepositoryImpl(
                jpaRepository,
                mock(MenuJpaRepository.class),
                mock(FoodCategoryJpaRepository.class),
                mock(VoteCandidateMapper.class),
                mock(MenuVoteCandidateMapper.class)
        );
    }
}
