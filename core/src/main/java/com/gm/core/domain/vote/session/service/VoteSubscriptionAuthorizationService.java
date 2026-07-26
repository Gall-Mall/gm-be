package com.gm.core.domain.vote.session.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;

/** 투표 세션 구독 사용자가 세션 소속 그룹의 활성 멤버인지 확인한다. */
@Service
@RequiredArgsConstructor
public class VoteSubscriptionAuthorizationService {

    private final GroupService groupService;
    private final VoteSessionRepository voteSessionRepository;

    @Transactional(readOnly = true)
    public void authorize(UUID voteSessionId, UUID userId) {
        Assert.notNull(voteSessionId, "voteSessionId must not be null");
        Assert.notNull(userId, "userId must not be null");
        VoteSession session = voteSessionRepository.findById(voteSessionId)
                .orElseThrow(() -> new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
        groupService.findGroupDetail(session.diningGroupId(), userId);
    }
}
