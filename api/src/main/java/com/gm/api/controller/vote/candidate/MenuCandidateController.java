package com.gm.api.controller.vote.candidate;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.vote.candidate.dto.response.MenuCandidateResponse;
import com.gm.core.domain.vote.candidate.service.MenuCandidateService;

/**
 * 메뉴 투표 후보 조회 API를 제공한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vote-sessions/{voteSessionId}/menu-candidates")
public class MenuCandidateController {

    private final MenuCandidateService menuCandidateService;

    /**
     * 투표 세션의 추천 메뉴 후보를 노출 순서대로 조회한다.
     *
     * @param voteSessionId 투표 세션 식별자
     * @return 메뉴 정보와 최신 집계를 포함한 후보 목록
     */
    @GetMapping
    public ResponseEnvelope<List<MenuCandidateResponse>> findMenuCandidates(
            @PathVariable UUID voteSessionId
    ) {
        List<MenuCandidateResponse> candidates = menuCandidateService
                .findMenuCandidates(voteSessionId)
                .stream()
                .map(MenuCandidateResponse::from)
                .toList();
        return ResponseEnvelope.success(candidates);
    }
}
