package com.gm.core.domain.invite.exception;

import com.gm.core.exception.BusinessException;

/**
 * 초대 처리 중 발생한 오류를 나타낸다.
 */
public class InviteException extends BusinessException {

    /**
     * 지정한 오류 코드로 예외를 생성한다.
     *
     * @param inviteErrorCode 응답에 사용할 상태·코드·메시지
     */
    public InviteException(InviteErrorCode inviteErrorCode) {
        super(inviteErrorCode);
    }
}
