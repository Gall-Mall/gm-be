package com.gm.core.domain.group.model;

import java.time.LocalTime;
import java.util.UUID;

/**
 * 아직 저장되지 않은 그룹의 생성 명세이다.
 *
 * @param ownerUserId 그룹장이 될 요청 회원 식별자
 * @param name 그룹명
 * @param locationAddress 식당 검색 기준 주소
 * @param latitude 기준 위도
 * @param longitude 기준 경도
 * @param searchRadiusM 식당 검색 반경(m)
 * @param recommendationTime 추천 시간
 * @param maxMemberCount 최대 멤버 수
 */
public record NewGroup(
        UUID ownerUserId,
        String name,
        String locationAddress,
        Double latitude,
        Double longitude,
        int searchRadiusM,
        LocalTime recommendationTime,
        int maxMemberCount
) {
}
