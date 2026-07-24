package com.gm.core.domain.vote.session.model;

import com.gm.core.domain.vote.session.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.session.exception.VoteSessionException;
import lombok.Builder;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 한 끼 추천과 투표의 진행 상태 및 종합 선호 키워드를 보관하는 도메인 모델이다.
 *
 * @param id 세션 식별자
 * @param diningGroupId 세션이 속한 식사 그룹 식별자
 * @param voteSessionStatus 현재 세션 진행 상태
 * @param title 세션 제목
 * @param likeKeyword 종합 선호 키워드
 * @param dislikeKeyword 종합 비선호 키워드
 * @param startedAt 투표 시작 시각
 * @param closedAt 투표 종료 시각
 * @param createdAt 세션 생성 시각
 * @param updatedAt 세션 최종 수정 시각
 */
@Builder(toBuilder = true)
public record VoteSession(
        UUID id,
        UUID diningGroupId,
        VoteSessionStatus voteSessionStatus,
        String title,
        String likeKeyword,
        String dislikeKeyword,
        // 투표 시작 시간
        LocalDateTime startedAt,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 그룹 식별자·상태·제목이 올바른지 확인한다.
     *
     * @throws IllegalArgumentException 그룹 식별자나 상태가 없거나 제목이 비어 있는 경우
     */
    public VoteSession {
        Assert.notNull(diningGroupId, "groupId must not be null");
        Assert.notNull(voteSessionStatus, "voteSessionStatus must not be null");
        Assert.hasText(title, "title must not be null");
    }

    /**
     * 수동 투표 세션을 선호 입력 상태로 생성한다.
     *
     * <p>새 세션은 {@link VoteSessionStatus#PREFERENCE_INPUT} 상태로 시작한다.</p>
     *
     * @param diningGroupId 세션이 속한 식사 그룹 식별자
     * @param title 세션 제목
     * @param likeKeyword 종합 선호 키워드
     * @param dislikeKeyword 종합 비선호 키워드
     * @return {@link VoteSessionStatus#PREFERENCE_INPUT} 상태의 신규 세션
     * @throws IllegalArgumentException 그룹 식별자가 없거나 제목이 비어 있는 경우
     */
    public static VoteSession createVoteSession(
            UUID diningGroupId,
            String title,
            String likeKeyword,
            String dislikeKeyword
    ) {
        return VoteSession.builder()
                .diningGroupId(diningGroupId)
                .voteSessionStatus(VoteSessionStatus.PREFERENCE_INPUT)
                .title(title)
                .likeKeyword(likeKeyword)
                .dislikeKeyword(dislikeKeyword)
                .build();
    }

    /**
     * 투표 상태를 변경한다.
     *
     * @param nextStatus 변경할 상태
     * @return 상태가 변경된 투표 세션
     * @throws VoteSessionException 변경할 수 없는 상태인 경우
     * @throws IllegalArgumentException 변경할 상태가 없는 경우
     */
    public VoteSession changeStatus(VoteSessionStatus nextStatus) {
        Assert.notNull(nextStatus, "nextStatus must not be null");

        if (nextStatus == VoteSessionStatus.CANCELLED
                || !voteSessionStatus.canTransitionTo(nextStatus)) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }

        return toBuilder()
                .voteSessionStatus(nextStatus)
                .build();
    }

    /**
     * 진행 중인 투표를 취소한다.
     *
     * @param cancelledAt 취소 시각
     * @return 취소된 투표 세션
     * @throws VoteSessionException 취소할 수 없는 상태인 경우
     * @throws IllegalArgumentException 취소 시각이 없는 경우
     */
    public VoteSession cancel(LocalDateTime cancelledAt) {
        Assert.notNull(cancelledAt, "cancelledAt must not be null");

        if (!voteSessionStatus.canTransitionTo(VoteSessionStatus.CANCELLED)) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }

        return toBuilder()
                .voteSessionStatus(VoteSessionStatus.CANCELLED)
                .closedAt(cancelledAt)
                .build();
    }
}
