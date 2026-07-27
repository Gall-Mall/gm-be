package com.gm.api.controller.store;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.store.dto.response.StoreResponse;
import com.gm.api.security.CustomUserPrincipal;
import com.gm.core.domain.store.StoreSelectionService;

/** 투표 세션의 식당 후보 조회와 최종 식당 선택 API를 제공한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups/{groupId}/vote-sessions/{voteSessionId}/stores")
public class StoreSelectionController {

    private final StoreSelectionService storeSelectionService;

    /** 인증된 그룹 멤버가 조회할 수 있는 식당 후보를 거리순으로 반환한다. */
    @GetMapping
    public ResponseEnvelope<List<StoreResponse>> findStores(
            @PathVariable UUID groupId,
            @PathVariable UUID voteSessionId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEnvelope.success(
                storeSelectionService.findResults(groupId, principal.getUserId(), voteSessionId)
                        .stream()
                        .map(StoreResponse::from)
                        .toList()
        );
    }

    /** 활성 방장이 식당 후보 하나를 최종 선택하고 투표 세션을 완료한다. */
    @PutMapping("/{externalPlaceId}/selection")
    public ResponseEnvelope<StoreResponse> selectStore(
            @PathVariable UUID groupId,
            @PathVariable UUID voteSessionId,
            @PathVariable String externalPlaceId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEnvelope.success(StoreResponse.from(
                storeSelectionService.selectFinalRestaurant(
                        groupId,
                        principal.getUserId(),
                        voteSessionId,
                        externalPlaceId,
                        LocalDateTime.now()
                )
        ));
    }
}
