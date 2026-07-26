package com.gm.core.domain.history.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gm.core.domain.history.model.PreviousHistoryRecord;

public interface PreviousHistoryRepository {

    /**
     * 요청 회원이 활성 멤버인 그룹의 완료 세션과 선택 식당을 최신순으로 조회한다.
     *
     * @param userId 요청 회원 식별자
     * @return 선택 식당 생성 시각 내림차순의 지난 기록 행
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
}
