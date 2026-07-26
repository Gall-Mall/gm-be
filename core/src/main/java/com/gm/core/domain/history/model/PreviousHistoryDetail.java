package com.gm.core.domain.history.model;

import java.util.UUID;

/**
 * 완료된 투표 세션의 그룹과 최종 선택 결과를 표현한 상세 모델이다.
 */
public record PreviousHistoryDetail(
        UUID groupId,
        String groupName,
        PreviousVoteSessionHistory voteSession
) {

    public static PreviousHistoryDetail from(PreviousHistoryRecord record) {
        return new PreviousHistoryDetail(
                record.groupId(),
                record.groupName(),
                PreviousVoteSessionHistory.from(record)
        );
    }
}
