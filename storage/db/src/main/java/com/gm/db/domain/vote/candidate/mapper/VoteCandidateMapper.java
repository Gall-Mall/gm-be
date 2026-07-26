package com.gm.db.domain.vote.candidate.mapper;

import com.gm.core.domain.vote.candidate.model.menu.VoteCandidate;
import com.gm.db.domain.vote.candidate.entity.VoteCandidateEntity;
import org.mapstruct.Mapper;

/**
 * 메뉴 투표 후보 도메인 모델과 JPA 엔티티를 상호 변환한다.
 */
@Mapper(componentModel = "spring")
public interface VoteCandidateMapper {

    /**
     * 후보 도메인을 JPA 엔티티로 변환한다.
     *
     * @param domain 변환할 후보 도메인
     * @return 변환된 후보 엔티티
     */
    VoteCandidateEntity toEntity(VoteCandidate domain);

    /**
     * 후보 엔티티를 도메인으로 변환한다.
     *
     * @param entity 변환할 후보 엔티티
     * @return 변환된 후보 도메인
     */
    VoteCandidate toDomain(VoteCandidateEntity entity);
}
