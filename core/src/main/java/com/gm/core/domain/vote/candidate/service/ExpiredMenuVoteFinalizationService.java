package com.gm.core.domain.vote.candidate.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.gm.core.domain.vote.session.service.VoteSessionService;

/**
 * DB에 기록된 {@code MENU_VOTING} 시작 시각을 기준으로 1시간이 지난 투표를 마감한다.
 *
 * <p>한 번에 처리할 개수를 제한하고 세션별 실패를 분리해, 특정 세션의 장애가
 * 다음 만료 세션의 처리를 막지 않도록 한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiredMenuVoteFinalizationService {

    private static final Duration MENU_VOTE_DURATION = Duration.ofHours(1);
    private static final int BATCH_SIZE = 100;

    private final VoteSessionService voteSessionService;
    private final MenuVoteFinalizationService menuVoteFinalizationService;

    /**
     * 현재 시각 기준으로 만료된 세션을 오래된 순서로 최대 100개 마감한다.
     *
     * <p>마감에 실패한 세션은 성공 건수에 포함하지 않고 다음 세션을 계속 처리한다.
     * 해당 세션이 {@code MENU_VOTING} 상태로 남아 있으면 다음 실행에서 다시 시도된다.</p>
     *
     * @param now 이번 실행에서 사용할 현재 시각
     * @return 마감 서비스가 정상적으로 끝난 세션 수
     */
    public int finalizeExpiredVotes(LocalDateTime now) {
        int completedCount = 0;
        // 한 주기의 작업량을 제한해 오래 걸린 실행이 다음 스케줄을 과도하게 늦추지 않게 한다.
        for (UUID voteSessionId : voteSessionService.findExpiredMenuVoting(
                now.minus(MENU_VOTE_DURATION),
                BATCH_SIZE
        )) {
            try {
                menuVoteFinalizationService.finalizeVote(voteSessionId);
                completedCount++;
            } catch (RuntimeException exception) {
                // 실패한 세션은 상태를 유지해 다음 실행에서 재시도하고, 나머지 세션은 계속 처리한다.
                log.warn("만료 메뉴 투표 마감 실패: voteSessionId={}", voteSessionId, exception);
            }
        }
        return completedCount;
    }
}
