package com.gm.core.domain.store.repository;

import com.gm.core.domain.store.model.Store;
import java.util.List;
import java.util.UUID;

public interface StoreRepository {
    public void saveAll(UUID voteSessionId, List<Store> store);

    /**
     * 투표 세션에 저장된 식당 후보를 조회한다.
     *
     * @param voteSessionId 조회할 투표 세션 식별자
     * @return 식당 후보 목록
     */
    List<Store> findAllByVoteSessionId(UUID voteSessionId);

    /**
     * 투표 세션에 저장된 식당을 최종 식당으로 확정한다.
     *
     * @param voteSessionId 투표 세션 식별자
     * @param externalPlaceId 외부 장소 식별자
     * @return 최종 선택된 식당
     * @throws com.gm.core.domain.store.exception.StoreException 대상 식당이 없는 경우
     */
    Store selectAsFinalRestaurant(UUID voteSessionId, String externalPlaceId);
}
