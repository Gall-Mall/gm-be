package com.gm.core.domain.vote.candidate.model;

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

    public MenuVoteSubmitResult {
        Assert.notNull(status, "status must not be null");
        Assert.isTrue(
                status == Status.SUCCESS ? submission != null : submission == null,
                "submission must exist only on success"
        );
    }

    public static MenuVoteSubmitResult success(MenuVoteSubmission submission) {
        return new MenuVoteSubmitResult(Status.SUCCESS, submission);
    }

    public static MenuVoteSubmitResult candidateNotFound() {
        return new MenuVoteSubmitResult(Status.CANDIDATE_NOT_FOUND, null);
    }

    public static MenuVoteSubmitResult voteClosed() {
        return new MenuVoteSubmitResult(Status.VOTE_CLOSED, null);
    }
}
