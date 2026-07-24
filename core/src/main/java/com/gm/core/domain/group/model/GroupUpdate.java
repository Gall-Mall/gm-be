package com.gm.core.domain.group.model;

import java.time.LocalTime;

/**
 * 그룹 설정 전체 교체 명세이다.
 *
 * @param name 그룹명
 * @param locationAddress 식당 검색 기준 주소
 * @param latitude 기준 위도
 * @param longitude 기준 경도
 * @param searchRadiusM 식당 검색 반경(m)
 * @param recommendationTime 추천 시간
 * @param maxMemberCount 최대 멤버 수
 */
public record GroupUpdate(
        String name,
        String locationAddress,
        Double latitude,
        Double longitude,
        int searchRadiusM,
        LocalTime recommendationTime,
        int maxMemberCount
) {
}
