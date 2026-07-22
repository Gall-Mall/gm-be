package com.gm.core.domain.invite.exception;

import lombok.Getter;

import com.gm.core.exception.ErrorCode;

/**
 * 초대(INVITE) 도메인에 속하는 오류 코드를 정의한다.
 */
@Getter
public enum InviteErrorCode implements ErrorCode {

    /** 초대 코드가 없거나 TTL이 만료된 경우 사용한다. */
    INVITE_INVALID(404, "INVITE-001", "유효하지 않거나 만료된 초대 코드입니다."),

    /** 이미 활성 그룹원인 사용자가 동일 그룹 초대로 가입하려는 경우 사용한다. */
    ALREADY_MEMBER(409, "INVITE-002", "이미 가입한 그룹입니다.");

    private final int status;
    private final String code;
    private final String message;

    InviteErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
