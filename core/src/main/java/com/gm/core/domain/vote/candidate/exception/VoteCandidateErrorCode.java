package com.gm.core.domain.vote.candidate.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.gm.core.exception.ErrorCode;

/**
 * 메뉴 후보 투표에서 사용하는 오류 코드다.
 */
@Getter
@RequiredArgsConstructor
public enum VoteCandidateErrorCode implements ErrorCode {

    CANDIDATE_NOT_FOUND(404, "VOTE-001", "메뉴 후보를 찾을 수 없습니다."),
    VOTE_ALREADY_CLOSED(409, "VOTE-002", "투표가 이미 종료되었습니다."),
    INVALID_VOTE_TYPE(400, "VOTE-003", "투표 값이 올바르지 않습니다."),
    FINAL_MENU_SELECTION_NOT_ALLOWED(409, "VOTE-004", "현재 후보를 최종 메뉴로 선택할 수 없습니다."),
    VOTE_CLOSE_NOT_ALLOWED(409, "VOTE-005", "한 명 이상 투표한 후 마감할 수 있습니다."),
    INVALID_RECOMMENDATIONS(400, "VOTE-006", "추천 메뉴 후보가 올바르지 않습니다."),
    VOTE_SNAPSHOT_NOT_FOUND(409, "VOTE-007", "투표 집계 정보를 찾을 수 없어 세션을 종료했습니다."),
    FINAL_VOTE_NOT_ALLOWED(409, "VOTE-008", "현재 최종 투표를 제출할 수 없습니다."),
    FINAL_VOTE_TIE_REQUIRED(409, "VOTE-009", "동점 최종 투표만 방장이 선택할 수 있습니다.");

    private final int status;
    private final String code;
    private final String message;
}
