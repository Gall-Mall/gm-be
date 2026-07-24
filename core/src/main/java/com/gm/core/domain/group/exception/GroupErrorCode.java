package com.gm.core.domain.group.exception;

import lombok.Getter;

import com.gm.core.exception.ErrorCode;

/**
 * 그룹(GROUP) 도메인에 속하는 오류 코드를 정의한다.
 */
@Getter
public enum GroupErrorCode implements ErrorCode {

    /** 요청한 groupId에 해당하는 그룹이 없는 경우 사용한다. */
    GROUP_NOT_FOUND(404, "GROUP-001", "그룹을 찾을 수 없습니다."),

    /** 활성 그룹원이 아닌 사용자가 그룹 리소스에 접근한 경우 사용한다. */
    NOT_GROUP_MEMBER(403, "GROUP-002", "해당 그룹의 멤버가 아닙니다."),

    /** 현재 인원이 maxMemberCount 이상인 그룹에 가입하려는 경우 사용한다. */
    GROUP_FULL(409, "GROUP-004", "그룹 정원이 가득 찼습니다."),

    /** 활성 상태의 세션이 있는 그룹에서 수동 세션 생성을 요청한 경우 사용한다. */
    ACTIVE_SESSION_EXISTS(409, "GROUP-005", "이미 진행 중인 투표가 있습니다."),

    /** 그룹장이 아닌 사용자가 그룹장 전용 작업을 요청한 경우 사용한다. */
    NOT_GROUP_OWNER(403, "GROUP-006", "그룹장만 수행할 수 있습니다."),

    /** 강퇴(KICKED)된 사용자가 같은 그룹에 재가입을 시도하는 경우 사용한다. */
    MEMBER_KICKED(403, "GROUP-007", "강퇴된 그룹에는 재가입할 수 없습니다."),

    /** 그룹 정보 수정 시 현재 활성 멤버 수보다 작은 maxMemberCount로 변경을 시도하는 경우 사용한다. */
    GROUP_CAPACITY_BELOW_ACTIVE_MEMBERS(409, "GROUP-008", "정원이 현재 활성 멤버 수보다 작을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;

    GroupErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
