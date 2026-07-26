package com.gm.core.domain.vote.candidate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gm.core.domain.vote.candidate.model.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MenuVoteResultPolicyTest {

    private final MenuVoteResultPolicy policy = new MenuVoteResultPolicy();

    @Test
    @DisplayName("응답자가 없으면 판정을 보류한다")
    void decide_withNoRespondents_returnsUnresolved() {
        assertThat(policy.decide(count(0, 0, 0, 0)))
                .isEqualTo(VoteCandidateResult.UNRESOLVED);
    }

    @Test
    @DisplayName("NO 없이 GO가 응답자의 엄격한 과반이면 확정한다")
    void decide_withStrictGoMajorityAndNoRejections_returnsConfirmed() {
        assertThat(policy.decide(count(2, 1, 0, 3)))
                .isEqualTo(VoteCandidateResult.CONFIRMED);
    }

    @Test
    @DisplayName("GO가 정확히 절반이면 확정하지 않고 후보로 유지한다")
    void decide_withExactlyHalfGo_returnsKept() {
        assertThat(policy.decide(count(2, 2, 0, 4)))
                .isEqualTo(VoteCandidateResult.KEPT);
    }

    @Test
    @DisplayName("NO가 응답자의 절반 이상이면 제외한다")
    void decide_withAtLeastHalfNo_returnsRejected() {
        assertThat(policy.decide(count(1, 1, 2, 4)))
                .isEqualTo(VoteCandidateResult.REJECTED);
    }

    @Test
    @DisplayName("확정과 제외 조건에 해당하지 않으면 후보로 유지한다")
    void decide_withNoTerminalCondition_returnsKept() {
        assertThat(policy.decide(count(1, 2, 0, 3)))
                .isEqualTo(VoteCandidateResult.KEPT);
    }

    private MenuVoteCount count(int go, int maybe, int no, int respondents) {
        return new MenuVoteCount(UUID.randomUUID(), go, maybe, no, respondents);
    }
}
