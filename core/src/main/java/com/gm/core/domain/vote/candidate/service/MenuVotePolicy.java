package com.gm.core.domain.vote.candidate.service;

import java.time.Duration;

/**
 * 메뉴 투표의 공통 진행 정책이다.
 */
public final class MenuVotePolicy {

    /** Redis 마감 시각과 DB 만료 조회에 함께 사용하는 투표 가능 시간. */
    public static final Duration VOTING_DURATION = Duration.ofHours(1);

    private MenuVotePolicy() {
    }
}
