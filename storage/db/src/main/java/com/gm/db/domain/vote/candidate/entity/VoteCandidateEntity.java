package com.gm.db.domain.vote.candidate.entity;

import com.gm.core.domain.vote.candidate.model.VoteCandidateResult;
import com.gm.db.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 메뉴 투표 후보를 {@code vote_candidate} 테이블에 매핑한다.
 */
@Entity
@Table(name = "vote_candidate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteCandidateEntity extends BaseEntity {

    @Column(nullable = false)
    private UUID voteSessionId;

    @Column(nullable = false)
    private UUID menuId;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean selected;

    private Integer goCount;

    private Integer maybeCount;

    private Integer noCount;

    private Integer respondentCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoteCandidateResult resultStatus;

    @Column(length = 255)
    private String description;

    /**
     * 새 메뉴 투표 후보 엔티티를 생성한다.
     *
     * @param voteSessionId 투표 세션 식별자
     * @param menuId 메뉴 식별자
     * @param displayOrder 세션 내 노출 순서
     * @param selected 최종 메뉴 선택 여부
     * @param goCount 갈래 응답 수
     * @param maybeCount 애매 응답 수
     * @param noCount 말래 응답 수
     * @param respondentCount 응답자 수
     * @param resultStatus 투표 판정 상태
     * @param description 추천 이유
     */
    public VoteCandidateEntity(
            UUID voteSessionId,
            UUID menuId,
            int displayOrder,
            boolean selected,
            Integer goCount,
            Integer maybeCount,
            Integer noCount,
            Integer respondentCount,
            VoteCandidateResult resultStatus,
            String description
    ) {
        this.voteSessionId = voteSessionId;
        this.menuId = menuId;
        this.displayOrder = displayOrder;
        this.selected = selected;
        this.goCount = goCount;
        this.maybeCount = maybeCount;
        this.noCount = noCount;
        this.respondentCount = respondentCount;
        this.resultStatus = resultStatus;
        this.description = description;
    }
}
