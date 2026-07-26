package com.gm.db.domain.store.repository;

import com.gm.core.domain.store.model.Store;
import com.gm.core.domain.store.repository.StoreRepository;
import com.gm.core.domain.store.exception.StoreErrorCode;
import com.gm.core.domain.store.exception.StoreException;
import com.gm.db.domain.store.entity.StoreEntity;
import com.gm.db.domain.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StoreRepositoryImpl implements StoreRepository {

    private final StoreJpaRepository storeJpaRepository;
    private final StoreMapper storeMapper;

    @Override
    public void saveAll(UUID voteSessionId, List<Store> stores) {
        storeJpaRepository.saveAll(stores.stream().map(store -> storeMapper.toStoreEntity(voteSessionId,store)).toList());
    }

    @Override
    @Transactional
    public void selectAsFinalRestaurant(UUID voteSessionId, String externalPlaceId) {
        StoreEntity store = storeJpaRepository
                .findByVoteSessionIdAndExternalPlaceId(voteSessionId, externalPlaceId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.RESTAURANT_NOT_FOUND));

        store.selectAsFinalRestaurant();
    }
}
