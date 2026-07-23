package com.gm.core.domain.group.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 저장이 완료된 그룹-회원 멤버십의 도메인 모델이다.
 *
 * @param groupMemberId 멤버십 식별자
 * @param groupId 그룹 식별자
 * @param userId 회원 식별자
 * @param role 그룹 내 역할
 * @param status 멤버 상태
 * @param createdAt 생성일시
 * @param updatedAt 수정일시
 */
public record GroupMember(
        UUID groupMemberId,
        UUID groupId,
        UUID userId,
        GroupMemberRole role,
        GroupMemberStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
