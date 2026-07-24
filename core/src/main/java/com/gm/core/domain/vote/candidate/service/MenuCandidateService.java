package com.gm.core.domain.vote.candidate.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.RecommendedMenuCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.session.repository.VoteSessionRepository;

/**
 * 추천 메뉴 후보를 저장하고 투표 화면용 후보를 조회한다.
 */
@Service
@RequiredArgsConstructor
public class MenuCandidateService {

    private final VoteSessionRepository voteSessionRepository;
    private final VoteCandidateRepository voteCandidateRepository;

    /**
     * 추천 결과를 저장하고 메뉴 투표를 시작한다.
     * 후보 저장과 세션 상태 변경은 같은 트랜잭션에서 처리한다.
     *
     * @param voteSessionId 투표 세션 식별자
     * @param recommendations 추천된 메뉴 목록
     * @return 저장된 메뉴 후보
     */
    @Transactional
    public List<VoteCandidate> completeRecommendation(
            UUID voteSessionId,
            List<RecommendedMenuCandidate> recommendations
    ) {
        Assert.notEmpty(recommendations, "recommendations must not be empty");
        VoteSession session = findVoteSession(voteSessionId);
        session.changeStatus(VoteSessionStatus.MENU_VOTING);

        List<VoteCandidate> candidates = recommendations.stream()
                .map(recommendation -> VoteCandidate.builder()
                        .voteSessionId(voteSessionId)
                        .menuId(recommendation.menuId())
                        .displayOrder(recommendation.displayOrder())
                        .selected(false)
                        .goCount(0)
                        .maybeCount(0)
                        .noCount(0)
                        .respondentCount(0)
                        .resultStatus(VoteCandidateResult.PENDING)
                        .description(recommendation.description())
                        .build())
                .toList();

        List<VoteCandidate> saved = voteCandidateRepository.saveAll(candidates);
        voteSessionRepository.updateStatus(voteSessionId, VoteSessionStatus.MENU_VOTING)
                .orElseThrow(() -> new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
        return saved;
    }

    /**
     * 투표 화면에 표시할 메뉴 후보를 조회한다.
     *
     * @param voteSessionId 투표 세션 식별자
     * @return 노출 순서대로 정렬된 메뉴 후보
     */
    @Transactional(readOnly = true)
    public List<MenuVoteCandidate> findMenuCandidates(UUID voteSessionId) {
        findVoteSession(voteSessionId);
        return voteCandidateRepository.findAllByVoteSessionId(voteSessionId);
    }

    private VoteSession findVoteSession(UUID voteSessionId) {
        return voteSessionRepository.findById(voteSessionId)
                .orElseThrow(() -> new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
    }
}
