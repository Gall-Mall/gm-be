package com.gm.core.domain.vote.session.exception;

import com.gm.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 투표 세션에서 사용하는 오류 코드다.
 */
@Getter
@RequiredArgsConstructor
public enum VoteSessionErrorCode implements ErrorCode {

    SESSION_NOT_FOUND(404, "SESSION-001", "투표를 찾을 수 없습니다."),
    ACTIVE_SESSION_NOT_FOUND(404, "SESSION-002", "진행 중인 투표가 없습니다."),
    INVALID_SESSION_STATUS(409, "SESSION-003", "현재 투표 단계에서는 요청을 처리할 수 없습니다."),
    PREFERENCE_INPUT_CLOSED(409, "SESSION-004", "선호 음식 입력이 이미 종료되었습니다.");

    private final int status;
    private final String code;
    private final String message;
}