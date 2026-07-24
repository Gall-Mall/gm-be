package com.gm.core.domain.store;

import com.gm.core.domain.group.exception.GroupErrorCode;
import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.model.GroupDetail;
import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.store.model.Coordinate;
import com.gm.core.domain.store.model.Store;
import com.gm.core.domain.store.repository.StoreRepository;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.service.VoteSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
     */
    public List<Store> searchNearby(UUID userId, UUID voteSessionId, String keyword, Coordinate center, int radius) {
        VoteSession voteSession = voteSessionService.findVoteSession(voteSessionId);
        UUID diningGroupId = voteSession.diningGroupId();
        GroupDetail groupDetail = groupService.findGroupDetail(diningGroupId, userId);

        if(groupDetail.currentUserRole() != GroupMemberRole.OWNER) {
            throw new GroupException(GroupErrorCode.NOT_GROUP_OWNER);
        }
        List<Store> stores = storeSearchPort.searchNearby(keyword, center, radius);
        storeRepository.saveAll(voteSessionId, stores);
        return stores;
    }
}
