package com.gm.core.domain.store;

import com.gm.core.domain.store.model.Coordinate;
import com.gm.core.domain.store.model.Store;
import java.util.List;

public interface StoreSearchPort {
    List<Store> searchNearby(String keyword, Coordinate center, int radius);
}
