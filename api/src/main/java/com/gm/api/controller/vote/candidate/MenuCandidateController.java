package com.gm.api.controller.vote.candidate;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.vote.candidate.dto.request.MenuVoteRequest;
import com.gm.api.controller.vote.candidate.dto.response.FinalMenuSelectionResponse;
import com.gm.api.controller.vote.candidate.dto.response.FinalMenuVoteResponse;
import com.gm.api.controller.vote.candidate.dto.response.MenuCandidateResponse;
import com.gm.api.controller.vote.candidate.dto.response.MenuVoteResultResponse;
import com.gm.api.controller.vote.candidate.dto.response.MenuVoteSubmissionResponse;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.vote.candidate.service.FinalMenuSelectionService;
import com.gm.core.domain.vote.candidate.service.MenuCandidateService;
import com.gm.core.domain.vote.candidate.service.MenuVoteFinalizationService;
import com.gm.core.domain.vote.candidate.service.MenuVoteService;

/** 메뉴 투표 후보 조회, 선택 제출, 수동 마감 및 최종 메뉴 선택 API를 제공한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates")
public class MenuCandidateController {

    private final MenuCandidateService menuCandidateService;
    private final MenuVoteService menuVoteService;
    private final MenuVoteFinalizationService menuVoteFinalizationService;
    private final FinalMenuSelectionService finalMenuSelectionService;

    /** 투표 세션의 추천 메뉴 후보를 노출 순서대로 조회한다. */
    @GetMapping
    public ResponseEnvelope<List<MenuCandidateResponse>> findMenuCandidates(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID groupId,
            @PathVariable UUID voteSessionId
    ) {
        List<MenuCandidateResponse> candidates = menuCandidateService
                .findMenuCandidates(groupId, principal.getUserId(), voteSessionId)
                .stream()
                .map(MenuCandidateResponse::from)
                .toList();
        return ResponseEnvelope.success(candidates);
    }

    /** 인증된 활성 그룹 멤버의 메뉴 후보 선택을 반영한다. */
    @PutMapping("/{candidateId}/vote")
    public ResponseEnvelope<MenuVoteSubmissionResponse> submitMenuVote(
            @PathVariable UUID groupId,
            @PathVariable UUID voteSessionId,
            @PathVariable UUID candidateId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody MenuVoteRequest request
    ) {
        return ResponseEnvelope.success(MenuVoteSubmissionResponse.from(menuVoteService.submitVote(
                groupId,
                voteSessionId,
                candidateId,
                principal.getUserId(),
                request.choice()
        )));
    }

    /** 활성 방장이 응답자가 있는 메뉴 투표를 조기 마감한다. */
    @PutMapping("/close")
    public ResponseEnvelope<List<MenuVoteResultResponse>> finalizeMenuVote(
            @PathVariable UUID groupId,
            @PathVariable UUID voteSessionId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        List<MenuVoteResultResponse> results = menuVoteFinalizationService.finalizeVoteManually(
                        groupId,
                        principal.getUserId(),
                        voteSessionId
                )
                .stream()
                .map(MenuVoteResultResponse::from)
                .toList();
        return ResponseEnvelope.success(results);
    }

    /** 두 후보 최종 투표에서 활성 그룹원의 선택을 제출한다. */
    @PutMapping("/{candidateId}/final-vote")
    public ResponseEnvelope<FinalMenuVoteResponse> submitFinalMenuVote(
            @PathVariable UUID groupId,
            @PathVariable UUID voteSessionId,
            @PathVariable UUID candidateId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEnvelope.success(FinalMenuVoteResponse.from(
                finalMenuSelectionService.submitFinalVote(
                        groupId,
                        principal.getUserId(),
                        voteSessionId,
                        candidateId
                )
        ));
    }

    /** 1개·3개 후보 또는 두 후보 동점에서 활성 방장이 최종 메뉴를 선택한다. */
    @PutMapping("/{candidateId}/final-selection")
    public ResponseEnvelope<FinalMenuSelectionResponse> selectFinalMenu(
            @PathVariable UUID groupId,
            @PathVariable UUID voteSessionId,
            @PathVariable UUID candidateId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEnvelope.success(FinalMenuSelectionResponse.from(
                finalMenuSelectionService.selectByOwner(
                        groupId,
                        principal.getUserId(),
                        voteSessionId,
                        candidateId
                )
        ));
    }

    /** 후보가 하나만 남았을 때 활성 방장이 재추천을 선택한다. */
    @PutMapping("/re-recommend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reRecommendSingleCandidate(
            @PathVariable UUID groupId,
            @PathVariable UUID voteSessionId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        finalMenuSelectionService.reRecommendSingleCandidate(
                groupId,
                principal.getUserId(),
                voteSessionId
        );
    }
}
