package com.gm.api.controller.vote.candidate;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.vote.candidate.dto.request.MenuVoteRequest;
import com.gm.api.controller.vote.candidate.dto.response.MenuCandidateResponse;
import com.gm.api.controller.vote.candidate.dto.response.MenuVoteResultResponse;
import com.gm.api.controller.vote.candidate.dto.response.MenuVoteSubmissionResponse;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.vote.candidate.service.MenuCandidateService;
import com.gm.core.domain.vote.candidate.service.MenuVoteFinalizationService;
import com.gm.core.domain.vote.candidate.service.MenuVoteService;

/** 메뉴 투표 후보 조회, 선택 제출, 수동 마감 API를 제공한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups/{groupId}/vote-sessions/{voteSessionId}/menu-candidates")
public class MenuCandidateController {

    private final MenuCandidateService menuCandidateService;
    private final MenuVoteService menuVoteService;
    private final MenuVoteFinalizationService menuVoteFinalizationService;

    /**
     * 투표 세션의 추천 메뉴 후보를 노출 순서대로 조회한다.
     *
     * @param principal 요청 회원
     * @param groupId 세션이 속한 그룹 식별자
     * @param voteSessionId 투표 세션 식별자
     * @return 메뉴 정보와 최신 집계를 포함한 후보 목록
     */
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

    /**
     * 인증된 활성 그룹 멤버의 메뉴 후보 선택을 반영한다.
     *
     * @param groupId 세션이 속한 그룹 식별자
     * @param voteSessionId 투표 세션 식별자
     * @param candidateId 선택할 메뉴 후보 식별자
     * @param principal 선택을 제출한 인증 회원
     * @param request 갈래·애매·말래 중 선택한 값
     * @return 반영된 선택과 해당 후보의 최신 집계
     */
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

    /**
     * 활성 방장이 응답자가 있는 메뉴 투표를 조기 마감한다.
     *
     * @param groupId 세션이 속한 그룹 식별자
     * @param voteSessionId 마감할 투표 세션 식별자
     * @param principal 마감을 요청한 인증 회원
     * @return 후보별 최종 집계와 판정 결과
     */
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
}
