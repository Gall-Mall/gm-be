package com.gm.core.domain.store;

import com.gm.core.domain.group.exception.GroupErrorCode;
import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.model.GroupDetail;
import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.store.model.Coordinate;
import com.gm.core.domain.store.model.Store;
import com.gm.core.domain.store.repository.StoreRepository;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.service.VoteSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 식당 검색은 지도 API 호출이 있어 MQ로 비동기 처리한다.
 * 검증은 요청 시점(동기)에, 실제 검색·저장은 리스너(비동기)에서 수행한다.
 */
@Service
@RequiredArgsConstructor
public class StoreSearchService {

    private final StoreSearchPort storeSearchPort;
    private final StoreRepository storeRepository;
    private final VoteSessionService voteSessionService;
    private final GroupService groupService;

    /**
     * 검색을 요청할 수 있는 상태인지 확인한다.
     *
     * <p>비동기 처리 전에 동기로 호출해야 한다. 리스너에서 검증하면 예외가 DLQ로 갈 뿐
     * 요청자는 아무 응답도 받지 못한다.</p>
     *
     * @throws GroupException 요청자가 그룹장이 아닌 경우
     * @throws VoteSessionException 세션이 없거나 식당 검색 단계가 아닌 경우
     */
    @Transactional(readOnly = true)
    public void validateSearchable(UUID userId, UUID voteSessionId) {
        VoteSession voteSession = voteSessionService.findVoteSession(voteSessionId);
        GroupDetail groupDetail = groupService.findGroupDetail(voteSession.diningGroupId(), userId);

        if (groupDetail.currentUserRole() != GroupMemberRole.OWNER) {
            throw new GroupException(GroupErrorCode.NOT_GROUP_OWNER);
        }

        if (voteSession.voteSessionStatus() != VoteSessionStatus.RESTAURANT_SEARCHING) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }
    }

    /**
     * 지도 API로 주변 식당을 검색해 저장하고 세션을 다음 단계로 넘긴다.
     *
     * <p>결과가 비면 저장도 상태 전이도 하지 않는다. 반경을 넓혀 다시 요청할 수 있어야 한다.</p>
     *
     * @return 저장된 식당 목록. 결과가 없으면 빈 목록
     */
    @Transactional
    public List<Store> searchAndSave(UUID voteSessionId, String keyword, Coordinate center, int radius) {
        // 요청 시점과 처리 시점 사이에 상태가 바뀔 수 있어 한 번 더 확인한다.
        VoteSession voteSession = voteSessionService.findVoteSession(voteSessionId);
        if (voteSession.voteSessionStatus() != VoteSessionStatus.RESTAURANT_SEARCHING) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }

        List<Store> stores = storeSearchPort.searchNearby(keyword, center, radius);

        if (stores.isEmpty()) {
            return List.of();
        }

        storeRepository.saveAll(voteSessionId, stores);
        voteSessionService.changeVoteSessionStatus(voteSessionId, VoteSessionStatus.RESTAURANT_SELECTION);
        return stores;
    }
}
