package com.gm.db.domain.vote.session.repository;

import com.gm.db.domain.vote.session.entity.VoteSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 투표 세션 엔티티의 기본 JPA 저장·조회·삭제 기능을 제공한다.
 */
public interface VoteSessionJpaRepository extends JpaRepository<VoteSessionEntity, UUID> {
}
