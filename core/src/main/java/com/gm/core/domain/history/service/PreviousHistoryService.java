package com.gm.core.domain.history.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.gm.core.domain.history.model.PreviousGroupHistory;
import com.gm.core.domain.history.model.PreviousHistoryRecord;
import com.gm.core.domain.history.model.PreviousVoteSessionHistory;
import com.gm.core.domain.history.repository.PreviousHistoryRepository;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;

@Service
@RequiredArgsConstructor
public class PreviousHistoryService {

    private final PreviousHistoryRepository previousHistoryRepository;

    /**
     * 요청 회원의 지난 기록을 그룹별로 묶어 반환한다.
     *
     * <p>저장소가 선택 식당 생성 시각 내림차순으로 반환한 순서를 {@link LinkedHashMap}으로
     * 유지하므로, 그룹은 가장 최근 기록이 있는 순서로, 각 그룹의 세션도 최신순으로 정렬된다.</p>
     */
    @Transactional(readOnly = true)
    public List<PreviousGroupHistory> getPreviousHistory(UUID userId) {
        Assert.notNull(userId, "userId must not be null");

        List<PreviousHistoryRecord> records =
                previousHistoryRepository.findPreviousHistoryByUserId(userId);
        Map<GroupKey, List<PreviousVoteSessionHistory>> historyByGroup =
                new LinkedHashMap<>();

        for (PreviousHistoryRecord record : records) {
            GroupKey groupKey = new GroupKey(record.groupId(), record.groupName());
            historyByGroup
                    .computeIfAbsent(groupKey, ignored -> new ArrayList<>())
                    .add(PreviousVoteSessionHistory.from(record));
        }

        return historyByGroup.entrySet().stream()
                .map(entry -> new PreviousGroupHistory(
                        entry.getKey().groupId(),
                        entry.getKey().name(),
                        entry.getValue()
                ))
                .toList();
    }

    /**
     * 요청 회원이 접근할 수 있는 완료된 지난 기록을 투표 세션 식별자로 조회한다.
     */
    @Transactional(readOnly = true)
    public PreviousHistoryRecord getPreviousHistoryDetail(
            UUID userId,
            UUID voteSessionId
    ) {
        Assert.notNull(userId, "userId must not be null");
        Assert.notNull(voteSessionId, "voteSessionId must not be null");

        return previousHistoryRepository
                .findPreviousHistoryByUserIdAndVoteSessionId(userId, voteSessionId)
                .orElseThrow(() ->
                        new VoteSessionException(VoteSessionErrorCode.SESSION_NOT_FOUND));
    }

    private record GroupKey(UUID groupId, String name) {
    }
}
