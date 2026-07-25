package com.gm.db.domain.vote.session.repository;

import com.gm.db.domain.vote.session.entity.VoteSessionEntity;
import com.gm.core.domain.vote.session.model.VoteSessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 투표 세션 엔티티의 기본 JPA 저장·조회·삭제 기능을 제공한다.
 */
public interface VoteSessionJpaRepository extends JpaRepository<VoteSessionEntity, UUID> {

    /**
     * 자동·수동 마감의 상태 확인과 변경이 겹치지 않도록 세션 행을 쓰기 잠금으로 조회한다.
     *
     * @param id 잠글 투표 세션 식별자
     * @return 세션이 존재하면 잠금이 적용된 엔티티, 없으면 빈 값
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from VoteSessionEntity session where session.id = :id")
    Optional<VoteSessionEntity> findByIdForUpdate(@Param("id") UUID id);

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
