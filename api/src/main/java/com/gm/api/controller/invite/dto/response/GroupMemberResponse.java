package com.gm.api.controller.invite.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.gm.core.domain.group.model.GroupMember;
import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.model.GroupMemberStatus;

/**
 * 초대 코드로 그룹 가입(INVITE-003) 응답이다.
 *
 * @param groupMemberId 등록된 멤버십 식별자
 * @param groupId 가입한 그룹 식별자
 * @param userId 가입한 회원 식별자
 * @param role 그룹 내 역할 (가입 직후에는 항상 MEMBER)
 * @param status 멤버 상태 (가입 직후에는 항상 ACTIVE)
 * @param createdAt 가입일시
 * @param updatedAt 수정일시
 */
public record GroupMemberResponse(
        UUID groupMemberId,
        UUID groupId,
        UUID userId,
        GroupMemberRole role,
        GroupMemberStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static GroupMemberResponse from(GroupMember member) {
        return new GroupMemberResponse(
                member.groupMemberId(),
                member.groupId(),
                member.userId(),
                member.role(),
                member.status(),
                member.createdAt(),
                member.updatedAt()
        );
    }
}
