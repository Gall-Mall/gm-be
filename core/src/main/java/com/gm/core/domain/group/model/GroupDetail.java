package com.gm.core.domain.group.model;

/**
 * 그룹 정보와 조회를 요청한 회원의 해당 그룹 내 역할을 함께 담는다.
 *
 * @param group 그룹 정보
 * @param currentUserRole 조회를 요청한 회원의 역할
 */
public record GroupDetail(
        Group group,
        GroupMemberRole currentUserRole
) {
}
