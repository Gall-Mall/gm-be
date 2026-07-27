package com.gm.core.domain.history.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gm.core.domain.history.model.PreviousGroupHistory;
import com.gm.core.domain.history.model.PreviousHistoryDetail;
import com.gm.core.domain.history.model.PreviousHistoryRecord;
import com.gm.core.domain.history.model.PreviousMenuCandidateHistory;
import com.gm.core.domain.history.repository.PreviousHistoryRepository;
import com.gm.core.domain.vote.candidate.model.menu.VoteCandidateResult;
import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PreviousHistoryServiceTest {

    @Mock
    private PreviousHistoryRepository previousHistoryRepository;

    private PreviousHistoryService previousHistoryService;

    @BeforeEach
    void setUp() {
        previousHistoryService = new PreviousHistoryService(previousHistoryRepository);
    }

    @Test
    @DisplayName("조회된 행을 그룹별로 묶고 저장소의 최신순을 유지한다")
    void getPreviousHistory_groupsRecordsAndPreservesOrder() {
        UUID userId = UUID.randomUUID();
        UUID firstGroupId = UUID.randomUUID();
        UUID secondGroupId = UUID.randomUUID();
        UUID firstGroupNewestSessionId = UUID.randomUUID();
        UUID secondGroupSessionId = UUID.randomUUID();
        UUID firstGroupOlderSessionId = UUID.randomUUID();

        given(previousHistoryRepository.findPreviousHistoryByUserId(userId))
                .willReturn(List.of(
                        record(
                                firstGroupId,
                                "강남 점심 모임",
                                firstGroupNewestSessionId,
                                "최신 식당",
                                LocalDateTime.of(2026, 7, 25, 12, 0)
                        ),
                        record(
                                secondGroupId,
                                "판교 저녁 모임",
                                secondGroupSessionId,
                                "중간 식당",
                                LocalDateTime.of(2026, 7, 24, 18, 0)
                        ),
                        record(
                                firstGroupId,
                                "강남 점심 모임",
                                firstGroupOlderSessionId,
                                "이전 식당",
                                LocalDateTime.of(2026, 7, 23, 12, 0)
                        )
                ));

        List<PreviousGroupHistory> result =
                previousHistoryService.getPreviousHistory(userId);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().groupId()).isEqualTo(firstGroupId);
        assertThat(result.getFirst().voteSessions())
                .extracting(session -> session.voteSessionId())
                .containsExactly(firstGroupNewestSessionId, firstGroupOlderSessionId);
        assertThat(result.get(1).groupId()).isEqualTo(secondGroupId);
        assertThat(result.get(1).voteSessions())
                .extracting(session -> session.voteSessionId())
                .containsExactly(secondGroupSessionId);
        assertThat(result.getFirst().voteSessions().getFirst().name()).isEqualTo("최신 식당");
        assertThat(result.getFirst().voteSessions().getFirst().goCount()).isEqualTo(3);
        assertThat(result.getFirst().voteSessions().getFirst().maybeCount()).isEqualTo(1);
        assertThat(result.getFirst().voteSessions().getFirst().noCount()).isZero();
        verify(previousHistoryRepository).findPreviousHistoryByUserId(userId);
    }

    @Test
    @DisplayName("지난 기록이 없으면 빈 목록을 반환한다")
    void getPreviousHistory_returnsEmptyListWhenNoHistoryExists() {
        UUID userId = UUID.randomUUID();
        given(previousHistoryRepository.findPreviousHistoryByUserId(userId))
                .willReturn(List.of());

        List<PreviousGroupHistory> result =
                previousHistoryService.getPreviousHistory(userId);

        assertThat(result).isEmpty();
        verify(previousHistoryRepository).findPreviousHistoryByUserId(userId);
    }

    @Test
    @DisplayName("접근 가능한 완료 세션의 조회 행을 상세 모델로 변환한다")
    void getPreviousHistoryDetail_convertsRepositoryRecordToDetail() {
        UUID userId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        PreviousHistoryRecord record = record(
                UUID.randomUUID(),
                "강남 점심 모임",
                voteSessionId,
                "이자카야 하루",
                LocalDateTime.of(2026, 7, 25, 12, 0)
        );
        given(previousHistoryRepository
                .findPreviousHistoryByUserIdAndVoteSessionId(userId, voteSessionId))
                .willReturn(Optional.of(record));
        PreviousMenuCandidateHistory candidate = new PreviousMenuCandidateHistory(
                UUID.randomUUID(),
                "불고기",
                "https://example.com/bulgogi.jpg",
                1,
                true,
                3,
                1,
                0,
                4,
                VoteCandidateResult.CONFIRMED
        );
        given(previousHistoryRepository.findMenuCandidatesByVoteSessionId(voteSessionId))
                .willReturn(List.of(candidate));

        PreviousHistoryDetail result =
                previousHistoryService.getPreviousHistoryDetail(userId, voteSessionId);

        assertThat(result.groupId()).isEqualTo(record.groupId());
        assertThat(result.groupName()).isEqualTo(record.groupName());
        assertThat(result.voteSession().voteSessionId()).isEqualTo(voteSessionId);
        assertThat(result.voteSession().name()).isEqualTo(record.restaurantName());
        assertThat(result.voteSession().completedAt()).isEqualTo(record.completedAt());
        assertThat(result.menuCandidates()).containsExactly(candidate);
        verify(previousHistoryRepository)
                .findPreviousHistoryByUserIdAndVoteSessionId(userId, voteSessionId);
        verify(previousHistoryRepository).findMenuCandidatesByVoteSessionId(voteSessionId);
    }

    @Test
    @DisplayName("접근할 수 있는 지난 기록이 없으면 세션을 찾을 수 없다고 처리한다")
    void getPreviousHistoryDetail_withInvisibleSession_throwsSessionNotFound() {
        UUID userId = UUID.randomUUID();
        UUID voteSessionId = UUID.randomUUID();
        given(previousHistoryRepository
                .findPreviousHistoryByUserIdAndVoteSessionId(userId, voteSessionId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                previousHistoryService.getPreviousHistoryDetail(userId, voteSessionId))
                .isInstanceOfSatisfying(VoteSessionException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(VoteSessionErrorCode.SESSION_NOT_FOUND));
    }

    private PreviousHistoryRecord record(
            UUID groupId,
            String groupName,
            UUID voteSessionId,
            String restaurantName,
            LocalDateTime completedAt
    ) {
        return new PreviousHistoryRecord(
                groupId,
                groupName,
                voteSessionId,
                restaurantName,
                "https://place.map.kakao.com/" + voteSessionId,
                "서울특별시 강남구",
                37.5,
                127.0,
                300,
                voteSessionId.toString(),
                3,
                1,
                0,
                completedAt
        );
    }
}
