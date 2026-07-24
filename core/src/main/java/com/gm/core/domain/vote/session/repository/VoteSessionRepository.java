package com.gm.core.domain.vote.session.repository;

import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;

import java.time.LocalDateTime;
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
