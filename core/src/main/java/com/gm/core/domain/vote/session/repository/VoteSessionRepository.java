package com.gm.core.domain.vote.session.repository;

import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 투표 세션 저장소다.
 */
public interface VoteSessionRepository {

    /**
     * 신규 투표 세션을 저장한다.
     *
     * @param voteSession 저장할 신규 투표 세션
     * @return 저장된 투표 세션
     */
    VoteSession save(VoteSession voteSession);

    /**
     * 식별자로 투표 세션을 조회한다.
     *
     * @param id 조회할 투표 세션 식별자
     * @return 세션이 존재하면 해당 세션, 없으면 빈 값
     */
    Optional<VoteSession> findById(UUID id);

    /**
     * 처리가 끝날 때까지 세션을 잠근 채 조회한다.
     * 비동기 처리에서 상태 확인과 전이 사이에 다른 컨슈머가 끼어드는 것을 막는다.
     *
     * @param id 투표 세션 식별자
     * @return 잠긴 투표 세션
     */
    Optional<VoteSession> findByIdForUpdate(UUID id);

    /**
     * 투표 상태를 변경한다.
     *
     * @param voteSessionId 변경할 투표 세션 식별자
     * @param voteSessionStatus 변경할 상태
     * @return 세션이 존재하면 변경된 세션, 없으면 빈 값
     */
    Optional<VoteSession> updateStatus(
            UUID voteSessionId,
            VoteSessionStatus voteSessionStatus
    );

    /**
     * 메뉴 투표 상태와 자동 마감 기준 시작 시각을 함께 기록한다.
     *
     * @param voteSessionId 시작할 투표 세션 식별자
     * @param startedAt 메뉴 투표 시작 시각
     * @return 세션이 존재하면 변경된 세션, 없으면 빈 값
     */
    Optional<VoteSession> startMenuVoting(UUID voteSessionId, LocalDateTime startedAt);

    /**
     * 시작 시각이 마감 기준 이전인 메뉴 투표 세션을 오래된 순서로 제한 조회한다.
     *
     * @param cutoff 이 시각을 포함해 먼저 시작한 세션을 만료로 보는 기준
     * @param limit 한 번에 조회할 최대 세션 수
     * @return 오래된 순서로 정렬된 만료 세션 식별자
     */
    List<UUID> findExpiredMenuVoting(LocalDateTime cutoff, int limit);

    /**
     * 투표를 취소하고 종료 시각을 기록한다.
     *
     * @param voteSessionId 취소할 투표 세션 식별자
     * @param cancelledAt 취소 시각
     * @return 세션이 존재하면 취소된 세션, 없으면 빈 값
     */
    Optional<VoteSession> cancel(
            UUID voteSessionId,
            LocalDateTime cancelledAt
    );

    /**
     * 식별자에 해당하는 투표 세션을 영구 삭제한다.
     *
     * @param voteSessionId 삭제할 투표 세션 식별자
     */
    void deleteById(UUID voteSessionId);
}
