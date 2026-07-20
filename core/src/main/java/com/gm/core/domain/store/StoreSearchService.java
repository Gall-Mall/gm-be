package com.gm.core.domain.store;

import com.gm.core.domain.store.model.Coordinate;
import com.gm.core.domain.store.model.Store;
import com.gm.core.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreSearchService {

    private final StoreSearchPort storeSearchPort;
    private final StoreRepository storeRepository;

    public List<Store> searchNearby(String keyword, Coordinate center, int radius) {
        List<Store> stores = storeSearchPort.searchNearby(keyword, center, radius);
        storeRepository.saveAll(stores);
        return stores;
    }
}
