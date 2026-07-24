package com.gm.core.domain.vote.candidate.exception;

import com.gm.core.exception.BusinessException;

/**
 * 메뉴 후보 투표 규칙을 위반한 경우 발생한다.
 */
public class VoteCandidateException extends BusinessException {

    /**
     * 지정한 메뉴 후보 오류 코드로 예외를 생성한다.
     *
     * @param errorCode 응답에 사용할 상태·코드·메시지
     */
    public VoteCandidateException(VoteCandidateErrorCode errorCode) {
        super(errorCode);
    }
}
