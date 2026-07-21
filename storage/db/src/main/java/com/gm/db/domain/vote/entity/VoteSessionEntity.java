package com.gm.db.domain.vote.entity;

import com.gm.core.domain.vote.exception.VoteSessionErrorCode;
import com.gm.core.domain.vote.exception.VoteSessionException;
import com.gm.core.domain.vote.model.VoteSessionStatus;
import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 투표 세션을 {@code vote_session} 테이블에 매핑한다.
 */
@Entity
@Table(name = "vote_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteSessionEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID diningGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VoteSessionStatus voteSessionStatus;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 255)
    private String likeKeyword;

    @Column(length = 255)
    private String dislikeKeyword;

    private LocalDateTime startedAt;

    private LocalDateTime closedAt;

    /**
     * 새 투표 세션 엔티티를 생성한다.
     *
     * @param diningGroupId 세션이 속한 식사 그룹 식별자
     * @param voteSessionStatus 현재 세션 진행 상태
     * @param title 세션 제목
     * @param likeKeyword 종합 선호 키워드
     * @param dislikeKeyword 종합 비선호 키워드
     * @param startedAt 투표 시작 시각
     * @param closedAt 투표 종료 시각
     */
    public VoteSessionEntity(
            UUID diningGroupId,
            VoteSessionStatus voteSessionStatus,
            String title,
            String likeKeyword,
            String dislikeKeyword,
            LocalDateTime startedAt,
            LocalDateTime closedAt
    ) {
        this.diningGroupId = diningGroupId;
        this.voteSessionStatus = voteSessionStatus;
        this.title = title;
        this.likeKeyword = likeKeyword;
        this.dislikeKeyword = dislikeKeyword;
        this.startedAt = startedAt;
        this.closedAt = closedAt;
    }

    /**
     * 투표 상태를 변경한다.
     *
     * @param voteSessionStatus 변경할 상태
     */
    public void updateStatus(VoteSessionStatus voteSessionStatus) {
        this.voteSessionStatus = voteSessionStatus;
    }

    /**
     * 투표를 취소하고 종료 시각을 기록한다.
     *
     * @param cancelledAt 취소 시각
     */
    public void cancel(LocalDateTime cancelledAt) {
        if (!voteSessionStatus.canTransitionTo(VoteSessionStatus.CANCELLED)) {
            throw new VoteSessionException(VoteSessionErrorCode.INVALID_SESSION_STATUS);
        }

        this.voteSessionStatus = VoteSessionStatus.CANCELLED;
        this.closedAt = cancelledAt;
    }
}