package com.gm.api.controller.store;

import com.gm.api.security.CustomUserPrincipal;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gm.api.common.response.ResponseEnvelope;
import com.gm.api.controller.store.dto.request.StoreSearchRequest;
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
     * 지정한 좌표 주변의 음식점 검색을 요청한다.
     *
     * <p>지도 API 호출이 있어 비동기로 처리한다. 권한·세션 상태만 여기서 검증하고
     * 실제 검색은 store-search 리스너가 수행한다. 결과는 완료 이벤트로 전달된다.</p>
     *
     * @param request 투표 세션, 검색어, 중심 좌표와 검색 반경
     */
    @PostMapping("/search")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEnvelope<Void> searchNearby(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody StoreSearchRequest request
    ) {
        storeSearchService.requestSearch(
                principal.getUserId(),
                request.voteSessionId(),
                request.keyword(),
                request.toCoordinate(),
                request.radiusM()
        );

        return ResponseEnvelope.success(null);
    }
}
