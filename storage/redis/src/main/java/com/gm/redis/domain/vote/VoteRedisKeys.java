package com.gm.redis.domain.vote;

import java.util.UUID;

/** 메뉴 투표의 메타데이터, 선택, 집계를 보관하는 세션별 단일 Redis Hash 키를 만든다. */
final class VoteRedisKeys {

    private static final String PREFIX = "menu-vote:";

    private VoteRedisKeys() {
    }

    /** 세션 ID를 Redis Cluster hash tag로 감싸 세션 단위 키를 만든다. */
    static String session(UUID voteSessionId) {
        return PREFIX + "{" + voteSessionId + "}";
    }

    /** 두 후보 최종투표를 1차 투표와 분리된 단일 Hash 키로 만든다. */
    static String finalSession(UUID voteSessionId) {
        return "final-menu-vote:{" + voteSessionId + "}";
    }
}