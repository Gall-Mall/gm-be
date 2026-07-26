package com.gm.api.scheduler.vote;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gm.core.domain.vote.candidate.service.menuvote.ExpiredMenuVoteFinalizationService;

/**
 * API 인스턴스에서 DB 시작 시각 기준으로 만료된 메뉴 투표를 주기적으로 마감한다.
 *
 * <p>실행 간격은 이전 작업이 끝난 뒤부터 계산하는 고정 지연 방식이며,
 * 한 번 실행할 때 오래된 세션부터 최대 100개를 처리한다. 여러 API 인스턴스가
 * 동시에 실행해도 실제 마감은 세션 행 잠금으로 순서대로 처리된다.</p>
 */
@Component
@RequiredArgsConstructor
public class MenuVoteFinalizationScheduler {

    private final ExpiredMenuVoteFinalizationService expiredMenuVoteFinalizationService;

    /**
     * 설정된 간격마다 현재 시각 기준으로 30분이 지난 메뉴 투표를 마감한다.
     *
     * <p>개별 세션의 실패는 마감 서비스에서 분리해 처리하므로 같은 배치의
     * 다음 세션은 계속 진행된다. 실패한 세션은 DB 상태가 {@code MENU_VOTING}으로
     * 남아 다음 스케줄 실행 때 다시 조회된다.</p>
     */
    @Scheduled(fixedDelayString = "${gm.vote.menu-finalization-interval:PT1M}")
    public void finalizeExpiredVotes() {
        // 한 번의 실행에서 동일한 기준 시각을 사용해 만료 경계를 일관되게 계산한다.
        expiredMenuVoteFinalizationService.finalizeExpiredVotes(LocalDateTime.now());
    }
}
