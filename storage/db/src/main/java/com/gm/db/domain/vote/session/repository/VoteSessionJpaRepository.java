package com.gm.db.domain.vote.session.repository;

import com.gm.db.domain.vote.session.entity.VoteSessionEntity;
import jakarta.persistence.LockModeType;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 투표 세션 엔티티의 기본 JPA 저장·조회·삭제 기능을 제공한다.
 */
public interface VoteSessionJpaRepository extends JpaRepository<VoteSessionEntity, UUID> {

    /**
     * 비동기 처리에서 상태를 읽고 바꾸는 동안 다른 컨슈머가 끼어들지 못하게 잠근다.
     * 락 없이 읽으면 두 컨슈머가 같은 상태를 보고 각각 외부 API를 호출한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from VoteSessionEntity s where s.id = :id")
    Optional<VoteSessionEntity> findByIdForUpdate(@Param("id") UUID id);

    /**
     * 그룹의 종료 상태를 제외한 최신 투표 세션을 조회한다.
     */
    Optional<VoteSessionEntity> findFirstByDiningGroupIdAndVoteSessionStatusNotInOrderByCreatedAtDesc(
            UUID diningGroupId,
            List<VoteSessionStatus> terminalStatuses
    );

    /**
     * 마감 기준 시각이 지난 메뉴 투표 세션을 오래된 순서로 제한 조회한다.
     *
     * @param status 조회할 세션 상태
     * @param cutoff 이 시각을 포함해 먼저 시작한 세션을 조회하는 기준
     * @param pageable 한 번에 처리할 최대 개수
     * @return 오래된 순서로 정렬된 만료 세션
     */
    List<VoteSessionEntity> findAllByVoteSessionStatusAndStartedAtLessThanEqualOrderByStartedAtAsc(
            VoteSessionStatus status,
            LocalDateTime cutoff,
            Pageable pageable
    );
}
