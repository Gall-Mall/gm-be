package com.gm.db.domain.store.repository;

import com.gm.core.domain.store.model.Store;
import com.gm.core.domain.store.repository.StoreRepository;
import com.gm.db.domain.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
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
}
