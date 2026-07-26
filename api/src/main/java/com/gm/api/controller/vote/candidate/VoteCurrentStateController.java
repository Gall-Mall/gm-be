package com.gm.api.controller.vote.candidate;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.vote.candidate.dto.response.VoteCurrentStateResponse;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.vote.candidate.service.state.VoteCurrentStateService;

/** Socket 연결 전후에 클라이언트가 다시 동기화할 투표 세션 현재 상태 API를 제공한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups/{groupId}/vote-sessions/{voteSessionId}/vote-state")
public class VoteCurrentStateController {

    private final VoteCurrentStateService voteCurrentStateService;

    /** DB 기준 세션·후보·확정 메뉴와 Redis 기준 진행 중 투표·집계·deadline을 반환한다. */
    @GetMapping
    public ResponseEnvelope<VoteCurrentStateResponse> getCurrentState(
            @PathVariable UUID groupId,
            @PathVariable UUID voteSessionId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEnvelope.success(VoteCurrentStateResponse.from(
                voteCurrentStateService.getState(groupId, principal.getUserId(), voteSessionId)
        ));
    }
}
