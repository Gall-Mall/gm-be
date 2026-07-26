package com.gm.api.controller.vote.candidate.dto.response;

import java.util.UUID;

import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteChoice;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteCount;
import com.gm.core.domain.vote.candidate.model.menuvote.MenuVoteSubmission;

/** 메뉴 후보 투표 반영 결과. */
public record MenuVoteSubmissionResponse(
        UUID candidateId,
        MenuVoteChoice choice,
        Counts counts,
        int respondentCount,
        boolean changed
) {
    /** 반영된 메뉴 투표와 최신 집계를 API 응답으로 변환한다. */
    public static MenuVoteSubmissionResponse from(MenuVoteSubmission submission) {
        MenuVoteCount count = submission.count();
        return new MenuVoteSubmissionResponse(
                count.candidateId(),
                submission.choice(),
                new Counts(count.goCount(), count.maybeCount(), count.noCount()),
                count.respondentCount(),
                submission.changed()
        );
    }

    public record Counts(int go, int maybe, int no) {
    }
}
