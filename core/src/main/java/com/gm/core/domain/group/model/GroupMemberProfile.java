package com.gm.core.domain.group.model;

import java.util.UUID;

/**
 * 그룹 관리 화면에 표시할 활성 멤버 정보다.
 *
 * @param groupMemberId 멤버십 식별자
 * @param userId 회원 식별자
 * @param name 표시 이름
 * @param email 이메일
 * @param role 그룹 내 역할
 * @param status 멤버 상태
 */
public record GroupMemberProfile(
        UUID groupMemberId,
        UUID userId,
        String name,
        String email,
        GroupMemberRole role,
        GroupMemberStatus status
) {
}
