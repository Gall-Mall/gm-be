package com.gm.db.domain.store.repository;

import com.gm.core.domain.store.model.Store;
import com.gm.core.domain.store.model.Coordinate;
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

    /** {@inheritDoc} */
    @Override
    public List<Store> findAllByVoteSessionId(UUID voteSessionId) {
        return storeJpaRepository.findAllByVoteSessionIdOrderByDistanceAscIdAsc(voteSessionId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Store selectAsFinalRestaurant(UUID voteSessionId, String externalPlaceId) {
        StoreEntity store = storeJpaRepository
                .findByVoteSessionIdAndExternalPlaceId(voteSessionId, externalPlaceId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.RESTAURANT_NOT_FOUND));

        store.selectAsFinalRestaurant();
        return toDomain(store);
    }

    /**
     * 식당 저장 엔티티를 도메인 모델로 변환한다.
     *
     * @param entity 변환할 식당 엔티티
     * @return 식당 도메인 모델
     */
    private Store toDomain(StoreEntity entity) {
        return new Store(
                entity.getExternalPlaceId(),
                entity.getName(),
                entity.getAddress(),
                null,
                entity.getUrl(),
                new Coordinate(entity.getLongitude(), entity.getLatitude()),
                entity.getProvider(),
                String.valueOf(entity.getDistance())
        );
    }
}
