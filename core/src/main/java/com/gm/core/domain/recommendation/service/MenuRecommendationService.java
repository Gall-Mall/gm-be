package com.gm.core.domain.recommendation.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gm.core.domain.group.exception.GroupErrorCode;
import com.gm.core.domain.group.exception.GroupException;
import com.gm.core.domain.group.model.GroupDetail;
import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.service.GroupService;
import com.gm.core.domain.menu.model.Menu;
import com.gm.core.domain.menu.repository.MenuRepository;
import com.gm.core.domain.recommendation.model.CuratedCandidate;
import com.gm.core.domain.recommendation.model.GroupSoftSignals;
import com.gm.core.domain.recommendation.model.ScoredMenu;
import com.gm.core.domain.recommendation.repository.RecommendationRepository;
import com.gm.core.domain.vote.candidate.model.RecommendedMenuCandidate;
import com.gm.core.domain.vote.candidate.service.MenuCandidateService;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import com.gm.core.domain.vote.session.service.VoteSessionService;

/**
 * 메뉴 추천은 AI 호출이 있어 MQ로 비동기 처리한다.
 * 시작 요청은 동기로 검증·상태 전이만 하고, 실제 생성은 리스너에서 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuRecommendationService {

    /** 결정론 스코어링에서 AI에 넘길 후보 수. */
    private static final int CANDIDATE_POOL_SIZE = 30;

    /** 투표 화면에 올릴 최종 후보 수. */
    private static final int FINAL_CANDIDATE_COUNT = 10;

    private final GroupService groupService;
    private final VoteSessionService voteSessionService;
    private final RecommendationService recommendationService;
    private final RecommendationCurationService recommendationCurationService;
    private final MenuCandidateService menuCandidateService;
    private final RecommendationRepository recommendationRepository;
    private final MenuRepository menuRepository;

    /**
     * 방장이 추천을 시작한다. 검증 후 세션을 추천 단계로 넘긴다.
     *
     * <p>발행은 이 메서드가 하지 않는다. 커밋 후에 발행해야 하므로 호출자가 담당한다.</p>
     *
     * @return 추천 대상 그룹 식별자
     * @throws GroupException 요청자가 그룹장이 아닌 경우
     * @throws VoteSessionException 세션이 없거나 선호 입력 단계가 아닌 경우
     */
    @Transactional
    public UUID start(UUID userId, UUID voteSessionId) {
        VoteSession voteSession = voteSessionService.findVoteSession(voteSessionId);
        UUID groupId = voteSession.diningGroupId();
        GroupDetail groupDetail = groupService.findGroupDetail(groupId, userId);

        if (groupDetail.currentUserRole() != GroupMemberRole.OWNER) {
            throw new GroupException(GroupErrorCode.NOT_GROUP_OWNER);
        }

        if (voteSession.voteSessionStatus() != VoteSessionStatus.PREFERENCE_INPUT) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }

        // 여기서 전이해야 같은 요청이 두 번 들어와도 두 번째는 상태 검증에 걸린다.
        voteSessionService.changeVoteSessionStatus(voteSessionId, VoteSessionStatus.MENU_RECOMMENDING);
        return groupId;
    }

    /**
     * 결정론 스코어링 → AI 큐레이션 → 후보 저장까지 수행하고 메뉴 투표를 시작한다.
     *
     * <p>후보 저장과 MENU_VOTING 전이는 {@code MenuCandidateService}가 같은 트랜잭션에서 처리한다.</p>
     */
    @Transactional
    public void generate(UUID groupId, UUID voteSessionId) {
        List<ScoredMenu> scored = recommendationService.recommend(groupId, Set.of(), CANDIDATE_POOL_SIZE);
        if (scored.isEmpty()) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }

        Map<UUID, String> candidatesByMenuId = nameCandidates(scored);
        GroupSoftSignals signals = recommendationRepository.findSoftSignalsByGroupId(groupId);

        List<CuratedCandidate> curated = recommendationCurationService.curate(
                candidatesByMenuId,
                signals.allergenTexts(),
                signals.preferenceTexts(),
                signals.excludeFoodTexts(),
                FINAL_CANDIDATE_COUNT
        );

        // AI가 후보를 하나도 못 고르면 결정론 순위 상위를 그대로 쓴다. 추천 이유만 비게 된다.
        List<RecommendedMenuCandidate> candidates = curated.isEmpty()
                ? fallbackCandidates(candidatesByMenuId)
                : toCandidates(curated);

        if (curated.isEmpty()) {
            log.warn("[recommendation] AI 큐레이션 결과 없음 — 결정론 상위로 대체: session = {}", voteSessionId);
        }

        menuCandidateService.completeRecommendation(voteSessionId, candidates);
    }

    /** 점수 순서를 유지한 채 menuId를 이름으로 바꾼다. 마스터에 없는 id는 버린다. */
    private Map<UUID, String> nameCandidates(List<ScoredMenu> scored) {
        Map<UUID, String> nameById = new LinkedHashMap<>();
        for (Menu menu : menuRepository.findAll()) {
            nameById.put(menu.id(), menu.name());
        }

        Map<UUID, String> candidates = new LinkedHashMap<>();
        for (ScoredMenu menu : scored) {
            String name = nameById.get(menu.menuId());
            if (name != null) {
                candidates.put(menu.menuId(), name);
            }
        }
        return candidates;
    }

    private List<RecommendedMenuCandidate> toCandidates(List<CuratedCandidate> curated) {
        return IntStream.range(0, curated.size())
                .mapToObj(i -> new RecommendedMenuCandidate(
                        curated.get(i).menuId(), i + 1, curated.get(i).description()))
                .toList();
    }

    private List<RecommendedMenuCandidate> fallbackCandidates(Map<UUID, String> candidatesByMenuId) {
        List<UUID> menuIds = candidatesByMenuId.keySet().stream().limit(FINAL_CANDIDATE_COUNT).toList();
        return IntStream.range(0, menuIds.size())
                .mapToObj(i -> new RecommendedMenuCandidate(menuIds.get(i), i + 1, null))
                .toList();
    }
}
