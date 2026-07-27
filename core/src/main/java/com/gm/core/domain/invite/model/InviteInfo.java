package com.gm.core.domain.invite.model;

import java.util.UUID;
import java.time.LocalTime;

/**
 * 초대 코드가 가리키는 그룹 정보와 가입 가능 여부를 담는다.
 *
 * @param inviteCode 초대 코드
 * @param groupId 그룹 식별자
 * @param groupName 그룹명
 * @param ownerName 현재 방장 표시 이름
 * @param locationAddress 그룹 위치
 * @param recommendationTime 추천 시간
 * @param memberCount 현재 멤버 수
 * @param maxMemberCount 최대 멤버 수
 * @param joinable 가입 가능 여부 (정원 미달 여부)
 */
public record InviteInfo(
        String inviteCode,
        UUID groupId,
        String groupName,
        String ownerName,
        String locationAddress,
        LocalTime recommendationTime,
        int memberCount,
        int maxMemberCount,
        boolean joinable
) {
}
