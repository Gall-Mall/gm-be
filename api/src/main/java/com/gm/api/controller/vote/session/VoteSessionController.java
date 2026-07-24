package com.gm.api.controller.vote.session;

import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.vote.session.dto.request.VoteSessionCreateRequest;
import com.gm.api.controller.vote.session.dto.response.VoteSessionResponse;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.service.VoteSessionService;

/**
 * 투표 세션 생성 API를 제공한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups/{groupId}/vote-sessions")
public class VoteSessionController {

    private final VoteSessionService voteSessionService;

    /**
     * 수동 투표 세션을 생성한다.
     *
     * @param principal 요청 회원
     * @param groupId 세션이 속한 그룹 식별자
     * @param request 세션 생성 요청
     * @return 생성된 투표 세션 식별자와 초기 상태
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEnvelope<VoteSessionResponse> createManualVoteSession(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID groupId,
            @Valid @RequestBody VoteSessionCreateRequest request
    ) {
        VoteSession voteSession = voteSessionService.createManualVoteSession(
                groupId,
                principal.getUserId(),
                request.title(),
                request.likeKeyword(),
                request.dislikeKeyword()
        );

        return ResponseEnvelope.success(VoteSessionResponse.from(voteSession));
    }
}
