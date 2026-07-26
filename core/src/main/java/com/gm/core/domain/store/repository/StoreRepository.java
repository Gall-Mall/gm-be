package com.gm.core.domain.store.repository;

import com.gm.core.domain.store.model.Store;
import java.util.List;
import java.util.UUID;

public interface StoreRepository {
    public void saveAll(UUID voteSessionId, List<Store> store);

    /**
     * 투표 세션에 저장된 식당을 최종 식당으로 확정한다.
     *
     * @param voteSessionId 투표 세션 식별자
     * @param externalPlaceId 외부 장소 식별자
     * @throws com.gm.core.domain.store.exception.StoreException 대상 식당이 없는 경우
     */
    void selectAsFinalRestaurant(UUID voteSessionId, String externalPlaceId);
}
