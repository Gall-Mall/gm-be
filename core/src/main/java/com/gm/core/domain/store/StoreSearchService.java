package com.gm.core.domain.store;

import com.gm.core.domain.store.model.Coordinate;
import com.gm.core.domain.store.model.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreSearchService {

    private final StoreSearchPort storeSearchPort;

    public List<Store> searchNearby(String keyword, Coordinate center, int radius) {
        return storeSearchPort.searchNearby(keyword, center, radius);
    }
}
