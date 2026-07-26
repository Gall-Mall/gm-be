package com.gm.core.domain.vote.candidate.model;

import java.util.List;

import org.springframework.util.Assert;

/**
 * 임시 투표 저장소의 마감 결과다.
 *
 * @param status 마감 상태
 * @param snapshot 성공한 경우의 최종 집계
 */
public record MenuVoteCloseResult(
        Status status,
        List<MenuVoteCount> snapshot
) {
    public enum Status {
        SUCCESS,
        NO_RESPONSE,
        SNAPSHOT_NOT_FOUND
    }

    public MenuVoteCloseResult {
        Assert.notNull(status, "status must not be null");
        Assert.notNull(snapshot, "snapshot must not be null");
        snapshot = List.copyOf(snapshot);
        Assert.isTrue(
                status == Status.SUCCESS || snapshot.isEmpty(),
                "snapshot must be empty on failure"
        );
    }

    public static MenuVoteCloseResult success(List<MenuVoteCount> snapshot) {
        return new MenuVoteCloseResult(Status.SUCCESS, snapshot);
    }

    public static MenuVoteCloseResult noResponse() {
        return new MenuVoteCloseResult(Status.NO_RESPONSE, List.of());
    }

    public static MenuVoteCloseResult snapshotNotFound() {
        return new MenuVoteCloseResult(Status.SNAPSHOT_NOT_FOUND, List.of());
    }
}
