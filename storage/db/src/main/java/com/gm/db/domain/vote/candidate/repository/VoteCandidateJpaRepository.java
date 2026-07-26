package com.gm.db.domain.vote.candidate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.gm.db.domain.vote.candidate.entity.VoteCandidateEntity;

/**
 * 메뉴 투표 후보의 JPA 저장과 화면용 조회를 제공한다.
 */
public interface VoteCandidateJpaRepository extends JpaRepository<VoteCandidateEntity, UUID> {

    /** 세션 후보를 화면 노출 순서대로 조회한다. */
    List<VoteCandidateEntity> findAllByVoteSessionIdOrderByDisplayOrderAsc(UUID voteSessionId);

    /** 세션에서 최종 선택된 후보 하나를 조회한다. */
    Optional<VoteCandidateEntity> findFirstByVoteSessionIdAndSelectedTrue(UUID voteSessionId);

    /** 최종 선택 중 동시 변경을 막기 위해 세션 후보 전체를 순서대로 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select candidate from VoteCandidateEntity candidate "
            + "where candidate.voteSessionId = :voteSessionId order by candidate.displayOrder")
    List<VoteCandidateEntity> findAllByVoteSessionIdForUpdate(@Param("voteSessionId") UUID voteSessionId);
}
