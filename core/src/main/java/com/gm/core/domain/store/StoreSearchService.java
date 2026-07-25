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

@Service
@RequiredArgsConstructor
public class StoreSearchService {

    private final StoreSearchPort storeSearchPort;
    private final StoreRepository storeRepository;
    private final VoteSessionService voteSessionService;
    private final GroupService groupService;

    /**
     * 카카오 지도를 호출하는 메서드
     * 유효한 세션인지, 요청자가 group_owner인지 확인
     * voteSessionStatus가 RESTAURANT_SEARCHING 인지 검증
     * 검색 성공 후 VoteSessionStatus.RESTAURANT_SELECTION로 세션상태 변경
     */
    @Transactional
    public List<Store> searchNearby(UUID userId, UUID voteSessionId, String keyword, Coordinate center, int radius) {
        VoteSession voteSession = voteSessionService.findVoteSession(voteSessionId);
        UUID diningGroupId = voteSession.diningGroupId();
        GroupDetail groupDetail = groupService.findGroupDetail(diningGroupId, userId);

        if(groupDetail.currentUserRole() != GroupMemberRole.OWNER) {
            throw new GroupException(GroupErrorCode.NOT_GROUP_OWNER);
        }

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
