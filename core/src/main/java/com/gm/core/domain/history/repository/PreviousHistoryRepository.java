package com.gm.core.domain.history.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gm.core.domain.history.model.PreviousMenuCandidateHistory;
import com.gm.core.domain.history.model.PreviousHistoryRecord;

public interface PreviousHistoryRepository {

    /**
     * 요청 회원이 활성 멤버인 그룹의 완료 세션과 선택 식당을 최신순으로 조회한다.
     *
     * @param userId 요청 회원 식별자
     * @return 투표 세션 완료 시각 내림차순의 지난 기록 행
     */
    List<PreviousHistoryRecord> findPreviousHistoryByUserId(UUID userId);

    /**
     * 요청 회원이 조회할 수 있는 완료 세션의 지난 기록을 단건 조회한다.
     *
     * @param userId 요청 회원 식별자
     * @param voteSessionId 조회할 투표 세션 식별자
     * @return 접근 가능하고 최종 메뉴·식당이 선택된 완료 기록
     */
    Optional<PreviousHistoryRecord> findPreviousHistoryByUserIdAndVoteSessionId(
            UUID userId,
            UUID voteSessionId
    );

    /**
     * 완료 세션에 저장된 모든 메뉴 후보의 최종 집계를 노출 순서대로 조회한다.
     *
     * @param voteSessionId 조회할 완료 투표 세션 식별자
     * @return 메뉴 후보별 최종 투표 집계 목록
     */
    List<PreviousMenuCandidateHistory> findMenuCandidatesByVoteSessionId(UUID voteSessionId);
}
