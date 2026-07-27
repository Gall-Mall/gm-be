package com.gm.db.domain.group.repository;

import java.util.UUID;

import com.gm.core.domain.group.model.GroupMemberRole;
import com.gm.core.domain.group.model.GroupMemberStatus;

/**
 * 그룹 멤버십과 사용자 표시 정보를 함께 조회하는 투영이다.
 */
public interface GroupMemberProfileProjection {

    /** @return 그룹 멤버십 식별자 */
    UUID getGroupMemberId();

    /** @return 회원 식별자 */
    UUID getUserId();

    /** @return 화면에 표시할 회원 이름 */
    String getName();

    /** @return 회원 이메일 */
    String getEmail();

    /** @return 그룹 내 역할 */
    GroupMemberRole getRole();

    /** @return 그룹 멤버 상태 */
    GroupMemberStatus getStatus();
}
