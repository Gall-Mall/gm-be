package com.gm.core.domain.vote.candidate.model.menuvote;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MenuVoteStateTest {

    @Test
    @DisplayName("현재 상태는 모든 후보에 투표를 마친 사용자 목록을 제공한다")
    void state_exposesCompletedUserIds() {
        MenuVoteState state = new MenuVoteState(
                MenuVoteState.Status.OPEN,
                Instant.now(),
                List.of()
        );

        assertThat(state)
                .extracting("completedUserIds")
                .isEqualTo(List.of());
    }
}
