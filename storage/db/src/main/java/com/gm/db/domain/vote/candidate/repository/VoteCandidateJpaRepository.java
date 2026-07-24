package com.gm.db.domain.vote.candidate.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gm.db.domain.vote.candidate.entity.VoteCandidateEntity;

/**
 * 메뉴 투표 후보의 JPA 저장과 화면용 조회를 제공한다.
 */
public interface VoteCandidateJpaRepository extends JpaRepository<VoteCandidateEntity, UUID> {

    List<VoteCandidateEntity> findAllByVoteSessionIdOrderByDisplayOrderAsc(UUID voteSessionId);
}
