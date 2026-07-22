package com.gm.db.domain.group;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.gm.core.domain.group.model.GroupMember;

/**
 * {@link GroupMemberEntity}와 {@link GroupMember} 도메인 모델 간 변환을 담당한다.
 */
@Mapper(componentModel = "spring")
public interface GroupMemberMapper {

    /**
     * 엔티티를 도메인 모델로 변환한다. 엔티티의 {@code id}는 {@code groupMemberId}로,
     * {@code diningGroupId}는 {@code groupId}로 이름을 맞춰 매핑한다.
     */
    @Mapping(target = "groupMemberId", source = "id")
    @Mapping(target = "groupId", source = "diningGroupId")
    GroupMember toDomainModel(GroupMemberEntity entity);
}
