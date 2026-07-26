package com.gm.api.scheduler.vote;

import java.time.Instant;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gm.core.domain.vote.candidate.service.finalmenu.ExpiredFinalMenuVoteService;

/**
 * Redis deadline 인덱스에서 만료된 두 후보 최종투표를 1분 주기로 마감한다.
 * NCP 단일 API 서버 기준이므로 별도 분산 락 없이 Redis Lua의 상태 전환 원자성으로 제출 경합을 막는다.
 */
@Component
@RequiredArgsConstructor
public class FinalMenuVoteExpirationScheduler {

    private final ExpiredFinalMenuVoteService expiredFinalMenuVoteService;

    /**
     * 설정된 고정 지연마다 같은 기준 시각으로 만료 세션을 조회해 최대 100개를 처리한다.
     * Lua가 실제 Redis 서버 시간과 deadline을 다시 비교하므로 deadline 이전 세션은 닫히지 않는다.
     */
    @Scheduled(fixedDelayString = "${gm.vote.final-vote-expiration-interval:PT1M}")
    public void finalizeExpiredFinalVotes() {
        expiredFinalMenuVoteService.finalizeExpired(Instant.now());
    }
}
