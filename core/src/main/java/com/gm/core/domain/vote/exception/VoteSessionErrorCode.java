package com.gm.core.domain.vote.exception;

import com.gm.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 투표 세션과 메뉴 투표에서 사용하는 오류 코드다.
 */
@Getter
@RequiredArgsConstructor
public enum VoteSessionErrorCode implements ErrorCode {

    SESSION_NOT_FOUND(404, "SESSION-001", "투표를 찾을 수 없습니다."),
    ACTIVE_SESSION_NOT_FOUND(404, "SESSION-002", "진행 중인 투표가 없습니다."),
    INVALID_SESSION_STATUS(409, "SESSION-003", "현재 투표 단계에서는 요청을 처리할 수 없습니다."),
    PREFERENCE_INPUT_CLOSED(409, "SESSION-004", "선호 음식 입력이 이미 종료되었습니다."),
    CANDIDATE_NOT_FOUND(404, "VOTE-001", "메뉴 후보를 찾을 수 없습니다."),
    VOTE_ALREADY_CLOSED(409, "VOTE-002", "투표가 이미 종료되었습니다."),
    INVALID_VOTE_TYPE(400, "VOTE-003", "투표 값이 올바르지 않습니다."),
    FINAL_MENU_SELECTION_NOT_ALLOWED(409, "VOTE-004", "현재 후보를 최종 메뉴로 선택할 수 없습니다."),
    VOTE_CLOSE_NOT_ALLOWED(409, "VOTE-005", "한 명 이상 투표한 후 마감할 수 있습니다.");

    private final int status;
    private final String code;
    private final String message;
}