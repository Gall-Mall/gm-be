package com.gm.core.domain.vote.candidate.service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.gm.core.domain.vote.candidate.model.FinalMenuVoteCloseResult;
import com.gm.core.domain.vote.candidate.repository.FinalMenuVoteRepository;
import com.gm.core.domain.vote.event.VoteEventType;
import com.gm.core.domain.vote.event.VoteSocketEvent;
import com.gm.core.domain.vote.event.VoteSocketEventPublisher;

/** Redis deadline 인덱스에서 만료된 두 후보 최종투표를 조회해 후속 결정을 수행한다. */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExpiredFinalMenuVoteService {

    private static final int BATCH_SIZE = 100;

    private final FinalMenuVoteRepository finalMenuVoteRepository;
    private final FinalMenuSelectionService finalMenuSelectionService;
    private final VoteSocketEventPublisher voteSocketEventPublisher;

    /**
     * 기준 시각까지 만료된 최종투표를 오래된 순서로 조회해 원자적으로 마감한다.
     * 단독 1위는 기존 DB 잠금·선택 흐름으로 넘기고, 동점 또는 0응답은 방장 선택 대기 상태를 알린다.
     *
     * @param now 이번 스케줄 실행에서 공통으로 사용할 기준 시각
     */
    public void finalizeExpired(Instant now) {
        for (var voteSessionId : finalMenuVoteRepository.findExpired(now, BATCH_SIZE)) {
            try {
                FinalMenuVoteCloseResult result = finalMenuVoteRepository.closeExpired(voteSessionId);
                switch (result.status()) {
                    case UNIQUE_WINNER -> finalMenuSelectionService.selectExpiredWinner(
                            voteSessionId,
                            result.selectedCandidateId()
                    );
                    case OWNER_SELECTION_PENDING -> {
                        // Redis Lua가 집계를 고정한 뒤의 상태를 보내므로 클라이언트는 방장 선택 대기로 전환할 수 있다.
                        finalMenuVoteRepository.findState(voteSessionId).ifPresent(state ->
                                voteSocketEventPublisher.publish(VoteSocketEvent.now(
                                        VoteEventType.FINAL_MENU_VOTE_UPDATED,
                                        voteSessionId,
                                        state
                                ))
                        );
                        finalMenuVoteRepository.removeExpiration(voteSessionId);
                    }
                    case NOT_FOUND -> finalMenuVoteRepository.removeExpiration(voteSessionId);
                    case NOT_DUE -> {
                        // Sorted Set score와 Redis TIME의 경계 차이일 수 있으므로 다음 주기에 다시 확인한다.
                    }
                }
            } catch (RuntimeException exception) {
                log.error("만료 최종투표 처리 실패: voteSessionId={}", voteSessionId, exception);
            }
        }
    }
}
