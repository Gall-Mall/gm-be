package com.gm.core.domain.group.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 저장이 완료된 그룹의 도메인 모델이다.
 *
 * @param id 그룹 식별자
 * @param ownerUserId 그룹장 회원 식별자
 * @param name 그룹명
 * @param locationAddress 식당 검색 기준 주소
 * @param latitude 기준 위도
 * @param longitude 기준 경도
 * @param searchRadiusM 식당 검색 반경(m)
 * @param recommendationTime 추천 시간
 * @param maxMemberCount 최대 멤버 수
 * @param memberCount 현재 멤버 수
 * @param createdAt 생성일시
 * @param updatedAt 수정일시
 */
public record Group(
        UUID id,
        UUID ownerUserId,
        String name,
        String locationAddress,
        Double latitude,
        Double longitude,
        int searchRadiusM,
        LocalTime recommendationTime,
        int maxMemberCount,
        int memberCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
