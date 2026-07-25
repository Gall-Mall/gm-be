package com.gm.api.controller.store;

import java.util.List;

import com.gm.api.security.CustomUserPrincipal;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.store.dto.request.StoreSearchRequest;
import com.gm.api.controller.store.dto.response.StoreResponse;
import com.gm.core.domain.store.StoreSearchService;

/**
 * 외부 장소 검색과 투표 세션별 추천 식당 저장 API를 제공한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreSearchService storeSearchService;

    /**
     * 지정한 좌표 주변의 음식점을 검색.
     *
     * @param request 투표 세션, 검색어, 중심 좌표와 검색 반경
     * @return 외부 장소 검색 결과
     */
    @PostMapping("/search")
    public ResponseEnvelope<List<StoreResponse>> searchNearby(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody StoreSearchRequest request
    ) {
        List<StoreResponse> stores = storeSearchService.searchNearby(
                        principal.getUserId(),
                        request.voteSessionId(),
                        request.keyword(),
                        request.toCoordinate(),
                        request.radiusM()
                )
                .stream()
                .map(StoreResponse::from)
                .toList();

        return ResponseEnvelope.success(stores);
    }
}
