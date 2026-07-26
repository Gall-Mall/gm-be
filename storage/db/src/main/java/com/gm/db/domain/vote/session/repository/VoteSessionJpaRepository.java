package com.gm.db.domain.vote.session.repository;

import com.gm.db.domain.vote.session.entity.VoteSessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
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
}
