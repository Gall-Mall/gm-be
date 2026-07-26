package com.gm.core.domain.vote.candidate.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.candidate.model.VoteCurrentState;
import com.gm.core.domain.vote.candidate.repository.FinalMenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.MenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;

/** Socket 연결 직후 클라이언트가 기준 상태로 사용할 투표 세션 현재 정보를 조합한다. */
@Service
@RequiredArgsConstructor
public class VoteCurrentStateService {

    private final GroupService groupService;
    private final VoteSessionRepository voteSessionRepository;
    private final VoteCandidateRepository voteCandidateRepository;
    private final MenuVoteRepository menuVoteRepository;
    private final FinalMenuVoteRepository finalMenuVoteRepository;

    /**
     * 세션·후보·확정 메뉴는 DB에서, 진행 중 투표와 deadline·집계는 Redis에서 조회한다.
     * 세션의 실제 그룹과 경로 그룹이 일치하고 요청자가 ACTIVE 멤버일 때만 결과를 반환한다.
     */
    @Transactional(readOnly = true)
    public VoteCurrentState getState(UUID groupId, UUID userId, UUID voteSessionId) {
        VoteSession session = voteSessionRepository.findById(voteSessionId)
                .orElseThrow(() -> new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
        if (!groupId.equals(session.diningGroupId())) {
            throw new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND);
        }
        groupService.findGroupDetail(groupId, userId);
        return new VoteCurrentState(
                session.voteSessionStatus(),
                voteCandidateRepository.findAllByVoteSessionId(voteSessionId),
                menuVoteRepository.findState(voteSessionId),
                finalMenuVoteRepository.findState(voteSessionId),
                voteCandidateRepository.findSelectedCandidate(voteSessionId)
        );
    }
}
