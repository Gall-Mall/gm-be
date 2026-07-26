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
import com.gm.core.domain.recommendation.service.MenuRecommendationService;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.service.VoteSessionService;
import com.gm.core.event.EventPublisher;
import com.gm.core.event.payload.SurveyRequested;
import com.gm.core.transaction.AfterCommitExecutor;

/**
 * 투표 세션 생성 API를 제공한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups/{groupId}/vote-sessions")
public class VoteSessionController {

    private final VoteSessionService voteSessionService;
    private final MenuRecommendationService menuRecommendationService;
    private final EventPublisher eventPublisher;
    private final AfterCommitExecutor afterCommitExecutor;

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

    /**
     * 방장이 메뉴 추천을 시작한다.
     *
     * <p>AI 호출이 있어 비동기로 처리한다. 권한·세션 상태 검증과 상태 전이만 여기서 하고,
     * 후보 생성은 recommendation 리스너가 수행한다. 결과는 완료 이벤트로 전달된다.</p>
     *
     * @param principal 요청 회원
     * @param voteSessionId 추천을 시작할 세션 식별자
     */
    @PostMapping("/{voteSessionId}/recommendations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEnvelope<Void> startRecommendation(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID voteSessionId
    ) {
        UUID diningGroupId = menuRecommendationService.start(principal.getUserId(), voteSessionId);

        // 상태 전이가 커밋된 뒤에 발행해야 롤백된 요청의 이벤트가 나가지 않는다.
        afterCommitExecutor.execute(() ->
                eventPublisher.publish(new SurveyRequested(diningGroupId, voteSessionId)));

        return ResponseEnvelope.success(null);
    }
}
