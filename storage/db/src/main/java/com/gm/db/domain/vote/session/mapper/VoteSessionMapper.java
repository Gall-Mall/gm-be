package com.gm.db.domain.vote.session.mapper;

import com.gm.core.domain.vote.session.model.VoteSession;
import com.gm.db.domain.vote.session.entity.VoteSessionEntity;
import org.mapstruct.Mapper;

/**
 * 투표 세션 도메인 모델과 JPA 엔티티를 상호 변환한다.
 */
@Mapper(componentModel = "spring")
public interface VoteSessionMapper {

    /**
     * 도메인을 엔티티로 변환한다.
     *
     * @param domain 변환할 도메인
     * @return 변환된 엔티티
     */
    VoteSessionEntity toEntity(VoteSession domain);

    /**
     * 엔티티를 도메인으로 변환한다.
     *
     * @param entity 변환할 엔티티
     * @return 변환된 투표 세션
     */
    VoteSession toDomain(VoteSessionEntity entity);
}