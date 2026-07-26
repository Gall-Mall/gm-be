package com.gm.core.domain.vote.candidate.model.menuvote;

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

    /** 마감 상태에 맞는 스냅샷을 확인하고 변경할 수 없게 복사한다. */
    public MenuVoteCloseResult {
        Assert.notNull(status, "status must not be null");
        Assert.notNull(snapshot, "snapshot must not be null");
        snapshot = List.copyOf(snapshot);
        Assert.isTrue(
                status == Status.SUCCESS || snapshot.isEmpty(),
                "snapshot must be empty on failure"
        );
    }

    /** 고정된 최종 집계가 있는 마감 결과를 만든다. */
    public static MenuVoteCloseResult success(List<MenuVoteCount> snapshot) {
        return new MenuVoteCloseResult(Status.SUCCESS, snapshot);
    }

    /** 응답자가 없어 수동 마감을 거절한 결과를 만든다. */
    public static MenuVoteCloseResult noResponse() {
        return new MenuVoteCloseResult(Status.NO_RESPONSE, List.of());
    }

    /** 마감할 Redis 집계를 찾지 못한 결과를 만든다. */
    public static MenuVoteCloseResult snapshotNotFound() {
        return new MenuVoteCloseResult(Status.SNAPSHOT_NOT_FOUND, List.of());
    }
}
