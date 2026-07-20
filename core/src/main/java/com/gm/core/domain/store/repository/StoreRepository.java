package com.gm.core.domain.store.repository;

import com.gm.core.domain.store.model.Store;

import java.util.List;

public interface StoreRepository {
    public void saveAll(List<Store> store);
}
