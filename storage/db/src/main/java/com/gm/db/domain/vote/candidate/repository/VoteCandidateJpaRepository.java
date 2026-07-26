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

    List<VoteCandidateEntity> findAllByVoteSessionIdOrderByDisplayOrderAsc(UUID voteSessionId);

    Optional<VoteCandidateEntity> findFirstByVoteSessionIdAndSelectedTrue(UUID voteSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select candidate from VoteCandidateEntity candidate "
            + "where candidate.voteSessionId = :voteSessionId order by candidate.displayOrder")
    List<VoteCandidateEntity> findAllByVoteSessionIdForUpdate(@Param("voteSessionId") UUID voteSessionId);
}
