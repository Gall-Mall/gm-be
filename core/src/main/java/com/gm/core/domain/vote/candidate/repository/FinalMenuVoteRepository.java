package com.gm.core.domain.vote.candidate.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gm.core.domain.vote.candidate.model.FinalMenuVoteCloseResult;
import com.gm.core.domain.vote.candidate.model.FinalMenuVoteResult;
import com.gm.core.domain.vote.candidate.model.FinalMenuVoteState;

/** 두 후보 최종 투표의 Redis 전용 실시간 상태를 관리한다. */
public interface FinalMenuVoteRepository {

    /** 후보 두 개와 시작 시점의 활성 그룹원 수를 원자적으로 최초 한 번 저장한다. */
    void initialize(UUID voteSessionId, List<UUID> candidateIds, int eligibleVoterCount);

    /** 최초 응답, 멱등 재시도, 선택 변경과 전원 응답 마감을 하나의 원자 연산으로 처리한다. */
    FinalMenuVoteResult submit(UUID voteSessionId, UUID userId, UUID candidateId);

    /** 재접속 클라이언트가 사용할 현재 최종투표 상태와 집계를 조회한다. */
    Optional<FinalMenuVoteState> findState(UUID voteSessionId);

    /** deadline score가 기준 시각 이하인 세션을 제한 조회한다. */
    List<UUID> findExpired(Instant now, int limit);

    /** Redis TIME 기준 deadline이 지난 OPEN 투표만 원자적으로 마감한다. */
    FinalMenuVoteCloseResult closeExpired(UUID voteSessionId);

    /** 최종투표 마감 후 deadline 인덱스에서 세션을 제거한다. */
    void removeExpiration(UUID voteSessionId);

    /** 전원 응답 결과가 동점이고 해당 후보가 동점 후보인지 확인한다. */
    boolean isTiedCandidate(UUID voteSessionId, UUID candidateId);

    /** DB 최종 선택이 커밋된 뒤 실시간 상태를 제거한다. */
    void delete(UUID voteSessionId);
}
