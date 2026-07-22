package com.gm.db.domain.vote.mapper;

import com.gm.core.domain.vote.model.VoteCandidate;
import com.gm.db.domain.vote.entity.VoteCandidateEntity;
import org.mapstruct.Mapper;

/**
 * 메뉴 투표 후보 도메인 모델과 JPA 엔티티를 상호 변환한다.
 */
@Mapper(componentModel = "spring")
public interface VoteCandidateMapper {

    VoteCandidateEntity toEntity(VoteCandidate domain);

    VoteCandidate toDomain(VoteCandidateEntity entity);
}
