package com.gm.db.domain.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StoreJpaRepository extends JpaRepository<StoreEntity, UUID> {
}
