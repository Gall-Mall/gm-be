package com.gm.db.domain.store.repository;

import com.gm.db.domain.store.entity.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreJpaRepository extends JpaRepository<StoreEntity, UUID> {

    Optional<StoreEntity> findByVoteSessionIdAndExternalPlaceId(
            UUID voteSessionId,
            String externalPlaceId
    );
}
