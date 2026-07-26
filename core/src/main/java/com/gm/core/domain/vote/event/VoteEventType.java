package com.gm.core.domain.vote.event;

/** 클라이언트가 처리하는 최소 투표 실시간 이벤트 종류다. */
public enum VoteEventType {
    MENU_VOTE_UPDATED,
    MENU_VOTE_CLOSED,
    FINAL_MENU_VOTE_UPDATED,
    FINAL_MENU_SELECTED
}
