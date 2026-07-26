package com.gm.core.domain.vote.candidate.service.menuvote;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.menu.VoteCandidateResult;

/**
 * 후보별 실제 응답자 수를 기준으로 메뉴 투표의 최종 판정을 계산한다.
 */
@Component
public class MenuVoteResultPolicy {

    /**
     * 응답자가 없으면 판정을 보류하고, 확정·제외·유지 정책을 순서대로 적용한다.
     *
     * @param count 후보별 최종 집계
     * @return 후보 판정 결과
     */
    public VoteCandidateResult decide(MenuVoteCount count) {
        Assert.notNull(count, "count must not be null");

        if (count.respondentCount() == 0) {
            return VoteCandidateResult.UNRESOLVED;
        }
        if (count.goCount() * 2 > count.respondentCount() && count.noCount() == 0) {
            return VoteCandidateResult.CONFIRMED;
        }
        if (count.noCount() * 2 >= count.respondentCount()) {
            return VoteCandidateResult.REJECTED;
        }
        return VoteCandidateResult.KEPT;
    }
}
