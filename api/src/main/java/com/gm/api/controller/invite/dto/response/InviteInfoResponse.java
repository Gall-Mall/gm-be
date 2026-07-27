package com.gm.api.controller.invite.dto.response;

import java.time.LocalTime;
import java.util.UUID;

import com.gm.core.domain.invite.model.InviteInfo;

/**
 * 초대 정보 조회(INVITE-002) 응답이다.
 *
 * @param inviteCode 조회한 초대 코드
 * @param groupId 초대 코드가 가리키는 그룹 식별자
 * @param groupName 그룹명
 * @param ownerName 현재 방장 표시 이름
 * @param locationAddress 그룹 위치
 * @param recommendationTime 추천 시간
 * @param memberCount 현재 멤버 수
 * @param maxMemberCount 최대 멤버 수
 * @param joinable 가입 가능 여부 (정원 미달 여부)
 */
public record InviteInfoResponse(
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

    public static InviteInfoResponse from(InviteInfo info) {
        return new InviteInfoResponse(
                info.inviteCode(),
                info.groupId(),
                info.groupName(),
                info.ownerName(),
                info.locationAddress(),
                info.recommendationTime(),
                info.memberCount(),
                info.maxMemberCount(),
                info.joinable()
        );
    }
}
