package com.gm.core.domain.vote.candidate.model.menuvote;

import org.springframework.util.Assert;

/**
 * 임시 투표 저장소의 선택 반영 결과다.
 *
 * @param status 선택 반영 상태
 * @param submission 성공한 경우의 선택 및 집계
 */
public record MenuVoteSubmitResult(
        Status status,
        MenuVoteSubmission submission
) {
    public enum Status {
        SUCCESS,
        CANDIDATE_NOT_FOUND,
        VOTE_CLOSED
    }

    /** 처리 상태와 제출 결과의 조합이 올바른지 확인한다. */
    public MenuVoteSubmitResult {
        Assert.notNull(status, "status must not be null");
        Assert.isTrue(
                status == Status.SUCCESS ? submission != null : submission == null,
                "submission must exist only on success"
        );
    }

    /** 선택과 최신 집계가 반영된 결과를 만든다. */
    public static MenuVoteSubmitResult success(MenuVoteSubmission submission) {
        return new MenuVoteSubmitResult(Status.SUCCESS, submission);
    }

    /** 세션에 속하지 않은 후보를 제출한 결과를 만든다. */
    public static MenuVoteSubmitResult candidateNotFound() {
        return new MenuVoteSubmitResult(Status.CANDIDATE_NOT_FOUND, null);
    }

    /** 이미 닫힌 투표에 제출한 결과를 만든다. */
    public static MenuVoteSubmitResult voteClosed() {
        return new MenuVoteSubmitResult(Status.VOTE_CLOSED, null);
    }
}
