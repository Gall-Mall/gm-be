package com.gm.core.domain.vote.candidate.model;

/**
 * 메뉴 후보의 투표 판정 상태를 나타낸다.
 */
public enum VoteCandidateResult {

    /** 아직 투표 결과를 판정하지 않은 상태. */
    PENDING,

    /** 갈래 응답이 과반이어서 즉시 확정된 상태. */
    CONFIRMED,

    /** 갈래와 애매 응답의 합이 과반이어서 후보로 유지된 상태. */
    KEPT,

    /** 말래 응답이 과반이어서 제외된 상태. */
    REJECTED,

    /** 어느 판정 조건에도 해당하지 않은 상태. */
    UNRESOLVED
}
