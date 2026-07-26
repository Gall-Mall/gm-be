package com.gm.core.domain.vote.candidate.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateErrorCode;
import com.gm.core.domain.vote.candidate.exception.VoteCandidateException;
import com.gm.core.domain.vote.candidate.model.MenuVoteCandidate;
import com.gm.core.domain.vote.candidate.model.MenuVoteSession;
import com.gm.core.domain.vote.candidate.model.RecommendedMenuCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidate;
import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.core.domain.vote.candidate.repository.VoteCandidateRepository;
import com.gm.core.domain.vote.candidate.repository.MenuVoteRepository;
import com.gm.core.domain.vote.candidate.service.MenuVotePolicy;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.service.VoteSessionService;
import com.gm.core.transaction.AfterCommitExecutor;

/**
 * 추천 메뉴 후보를 저장하고 투표 화면용 후보를 조회한다.
 */
@Service
@RequiredArgsConstructor
public class MenuCandidateService {

    private static final int MAX_CANDIDATE_COUNT = 10;

    private final GroupService groupService;
    private final VoteSessionService voteSessionService;
    private final VoteCandidateRepository voteCandidateRepository;
    private final MenuVoteRepository menuVoteRepository;
    private final AfterCommitExecutor afterCommitExecutor;

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
        validateRecommendations(recommendations);

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

        List<VoteCandidate> saved = voteCandidateRepository.saveNewCandidates(candidates);
        voteSessionService.startMenuVoting(voteSessionId, LocalDateTime.now());
        MenuVoteSession menuVoteSession = new MenuVoteSession(
                voteSessionId,
                saved.stream().map(VoteCandidate::id).toList(),
                MenuVotePolicy.VOTING_DURATION
        );
        // @Transactional 메서드가 끝나야 커밋되므로, 롤백 시 Redis 투표만 남지 않게 커밋 후 초기화한다.
        afterCommitExecutor.execute(() -> menuVoteRepository.initialize(menuVoteSession));
        return saved;
    }

    /**
     * 투표 화면에 표시할 메뉴 후보를 조회한다.
     *
     * @param groupId 경로의 그룹 식별자
     * @param userId 조회를 요청한 회원 식별자
     * @param voteSessionId 투표 세션 식별자
     * @return 노출 순서대로 정렬된 메뉴 후보
     */
    @Transactional(readOnly = true)
    public List<MenuVoteCandidate> findMenuCandidates(
            UUID groupId,
            UUID userId,
            UUID voteSessionId
    ) {
        groupService.findGroupDetail(groupId, userId);
        VoteSession session = voteSessionService.findVoteSession(voteSessionId);
        if (!groupId.equals(session.diningGroupId())) {
            throw new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND);
        }
        return voteCandidateRepository.findAllByVoteSessionId(voteSessionId);
    }

    private void validateRecommendations(List<RecommendedMenuCandidate> recommendations) {
        if (recommendations == null
                || recommendations.isEmpty()
                || recommendations.size() > MAX_CANDIDATE_COUNT) {
            throw new VoteCandidateException(VoteCandidateErrorCode.INVALID_RECOMMENDATIONS);
        }

        Set<UUID> menuIds = new HashSet<>();
        Set<Integer> displayOrders = new HashSet<>();
        for (RecommendedMenuCandidate recommendation : recommendations) {
            if (recommendation == null
                    || !menuIds.add(recommendation.menuId())
                    || !displayOrders.add(recommendation.displayOrder())) {
                throw new VoteCandidateException(VoteCandidateErrorCode.INVALID_RECOMMENDATIONS);
            }
        }
    }
}
