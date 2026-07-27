package com.gm.db.domain.store.repository;

import com.gm.db.domain.store.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface StoreJpaRepository extends JpaRepository<StoreEntity, UUID> {

    Optional<StoreEntity> findByVoteSessionIdAndExternalPlaceId(
            UUID voteSessionId,
            String externalPlaceId
    );

    /**
     * 투표 세션의 식당 후보를 거리와 식별자 오름차순으로 조회한다.
     *
     * @param voteSessionId 조회할 투표 세션 식별자
     * @return 정렬된 식당 후보 엔티티 목록
     */
    List<StoreEntity> findAllByVoteSessionIdOrderByDistanceAscIdAsc(UUID voteSessionId);
}
