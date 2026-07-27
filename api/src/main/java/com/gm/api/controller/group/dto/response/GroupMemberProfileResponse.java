package com.gm.api.controller.group.dto.response;

import java.util.UUID;

import com.gm.core.domain.group.model.GroupMemberProfile;
import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.model.GroupMemberStatus;

/**
 * 그룹 관리 화면에 제공할 활성 멤버 응답이다.
 *
 * @param groupMemberId 멤버십 식별자
 * @param userId 회원 식별자
 * @param name 표시 이름
 * @param email 이메일
 * @param role 그룹 내 역할
 * @param status 멤버 상태
 */
public record GroupMemberProfileResponse(
        UUID groupMemberId,
        UUID userId,
        String name,
        String email,
        GroupMemberRole role,
        GroupMemberStatus status
) {

    /**
     * 그룹 멤버 도메인 모델을 API 응답으로 변환한다.
     *
     * @param member 변환할 멤버
     * @return 그룹 멤버 응답
     */
    public static GroupMemberProfileResponse from(GroupMemberProfile member) {
        return new GroupMemberProfileResponse(
                member.groupMemberId(),
                member.userId(),
                member.name(),
                member.email(),
                member.role(),
                member.status()
        );
    }
}
