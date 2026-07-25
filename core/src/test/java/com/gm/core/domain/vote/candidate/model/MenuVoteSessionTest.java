package com.gm.core.domain.vote.candidate.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MenuVoteSessionTest {

    @Test
    @DisplayName("초기화할 후보 목록은 외부 변경으로부터 보호한다")
    void create_copiesCandidateIds() {
        List<UUID> candidateIds = new ArrayList<>(List.of(UUID.randomUUID()));

        MenuVoteSession session = new MenuVoteSession(
                UUID.randomUUID(),
                candidateIds,
                Duration.ofMinutes(30)
        );
        candidateIds.clear();

        assertThat(session.candidateIds()).hasSize(1);
        assertThatThrownBy(() -> session.candidateIds().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("후보가 없는 메뉴 투표 세션은 열 수 없다")
    void create_withoutCandidates_throwsException() {
        assertThatThrownBy(() -> new MenuVoteSession(
                UUID.randomUUID(),
                List.of(),
                Duration.ofMinutes(30)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("candidateIds must not be empty");
    }

    @Test
    @DisplayName("중복 후보가 있는 메뉴 투표 세션은 열 수 없다")
    void create_withDuplicateCandidate_throwsException() {
        UUID candidateId = UUID.randomUUID();

        assertThatThrownBy(() -> new MenuVoteSession(
                UUID.randomUUID(),
                List.of(candidateId, candidateId),
                Duration.ofMinutes(30)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("candidateIds must not contain duplicates");
    }

    @Test
    @DisplayName("투표 가능 시간은 양수여야 한다")
    void create_withNonPositiveVotingDuration_throwsException() {
        assertThatThrownBy(() -> new MenuVoteSession(
                UUID.randomUUID(),
                List.of(UUID.randomUUID()),
                Duration.ZERO
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("votingDuration must be positive");
    }
}
