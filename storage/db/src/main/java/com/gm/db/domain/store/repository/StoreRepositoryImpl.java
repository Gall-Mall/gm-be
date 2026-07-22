package com.gm.db.domain.store.repository;

import com.gm.core.domain.store.model.Store;
import com.gm.core.domain.store.repository.StoreRepository;
import com.gm.db.domain.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StoreRepositoryImpl implements StoreRepository {

    private final StoreJpaRepository storeJpaRepository;
    private final StoreMapper storeMapper;

    @Override
    public void saveAll(List<Store> stores) {
        storeJpaRepository.saveAll(stores.stream().map(storeMapper::toStoreEntity).toList());
    }
}
