package com.gm.core.domain.store.repository;

import com.gm.core.domain.store.model.Store;
import java.util.List;
import java.util.UUID;

public interface StoreRepository {
    public void saveAll(UUID voteSessionId, List<Store> store);
}
