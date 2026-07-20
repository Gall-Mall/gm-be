package com.gm.db.domain.store;

import com.gm.core.domain.store.model.Store;
import com.gm.core.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StoreRepositoryImpl implements StoreRepository {

    private final StoreJpaRepository storeJpaRepository;


    @Override
    public void saveAll(List<Store> stores) {
        storeJpaRepository.saveAll(stores.stream().map(StoreEntity::from).toList());
    }
}
