package com.gm.core.domain.vote.candidate.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateErrorCode;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateException;
import com.gm.core.domain.vote.candidate.model.MenuVoteChoice;
import com.gm.core.domain.vote.candidate.model.MenuVoteSubmission;
import com.gm.core.domain.vote.candidate.repository.MenuVoteRepository;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;

/**
 * 메뉴 후보에 대한 사용자 선택을 검증하고 임시 투표 저장소에 반영한다.
 * 세션 상태와 후보 소속은 DB에서 확인하고, 진행 중인 선택과 집계는 Redis에만 보관한다.
 */
@Service
@RequiredArgsConstructor
public class MenuVoteService {

    private final GroupService groupService;
    private final VoteSessionRepository voteSessionRepository;
    private final VoteCandidateRepository voteCandidateRepository;
    private final MenuVoteRepository menuVoteRepository;

    /**
     * 메뉴 투표 중인 세션의 후보에 사용자의 선택을 반영한다.
     *
     * @param groupId 경로의 그룹 ID
     * @param voteSessionId 투표 세션 ID
     * @param candidateId 메뉴 후보 ID
     * @param userId 사용자 ID
     * @param choice 사용자 선택
     * @return 선택 반영 결과와 최신 후보 집계
     * @throws VoteSessionException 투표 세션이 없는 경우
     * @throws VoteCandidateException 투표가 닫혔거나 세션에 속한 후보가 아닌 경우
     */
    @Transactional(readOnly = true)
    public MenuVoteSubmission submitVote(
            UUID groupId,
            UUID voteSessionId,
            UUID candidateId,
            UUID userId,
            MenuVoteChoice choice
    ) {
        Assert.notNull(groupId, "groupId must not be null");
        Assert.notNull(voteSessionId, "voteSessionId must not be null");
        Assert.notNull(candidateId, "candidateId must not be null");
        Assert.notNull(userId, "userId must not be null");
        Assert.notNull(choice, "choice must not be null");

        groupService.findGroupDetail(groupId, userId);
        VoteSession session = voteSessionRepository.findById(voteSessionId)
                .orElseThrow(() -> new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
        if (!groupId.equals(session.diningGroupId())) {
            throw new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND);
        }
        if (session.voteSessionStatus() != VoteSessionStatus.MENU_VOTING) {
            throw new VoteCandidateException(VoteCandidateErrorCode.VOTE_ALREADY_CLOSED);
        }

        boolean candidateExists = voteCandidateRepository.findAllByVoteSessionId(voteSessionId)
                .stream()
                .anyMatch(candidate -> candidate.voteCandidateId().equals(candidateId));
        if (!candidateExists) {
            throw new VoteCandidateException(VoteCandidateErrorCode.CANDIDATE_NOT_FOUND);
        }

        return menuVoteRepository.submit(voteSessionId, candidateId, userId, choice);
    }
}
