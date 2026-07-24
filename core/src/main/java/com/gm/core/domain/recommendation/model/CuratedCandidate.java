package com.gm.core.domain.recommendation.model;

import java.util.UUID;

/**
 * 화이트리스트 검증·UUID 매핑까지 끝난 큐레이션 최종 후보. vote_candidate에 저장된다.
 */
public record CuratedCandidate(
        UUID menuId,
        String description
) {
}
