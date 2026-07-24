package com.gm.api.controller.vote.session.dto.response;

import java.util.UUID;

import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;

/**
 * 생성된 투표 세션 정보이다.
 *
 * @param voteSessionId 투표 세션 식별자
 * @param status 투표 세션 상태
 */
public record VoteSessionResponse(
        UUID voteSessionId,
        VoteSessionStatus status
) {

    /**
     * 투표 세션 도메인을 생성 응답으로 변환한다.
     *
     * @param voteSession 생성된 투표 세션
     * @return 투표 세션 생성 응답
     */
    public static VoteSessionResponse from(VoteSession voteSession) {
        return new VoteSessionResponse(
                voteSession.id(),
                voteSession.voteSessionStatus()
        );
    }
}