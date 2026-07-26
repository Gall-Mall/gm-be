package com.gm.core.domain.history.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 지난 기록 조회에 필요한 그룹·세션·선택 식당 정보를 한 행으로 표현한다.
 *
 * @param groupId 식사 그룹 식별자
 * @param groupName 식사 그룹 이름
 * @param voteSessionId 투표 세션 식별자
 * @param restaurantName 선택된 식당 이름
 * @param url 선택된 식당 상세 URL
 * @param address 선택된 식당 주소
 * @param latitude 선택된 식당 위도
 * @param longitude 선택된 식당 경도
 * @param distanceM 그룹 기준 선택된 식당까지의 거리(미터)
 * @param externalPlaceId 외부 장소 식별자
 * @param goCount 최종 선택 메뉴의 갈래 응답 수
 * @param maybeCount 최종 선택 메뉴의 애매 응답 수
 * @param noCount 최종 선택 메뉴의 말래 응답 수
 * @param restaurantCreatedAt 선택된 식당 정보 생성 시각
 */
public record PreviousHistoryRecord(
        UUID groupId,
        String groupName,
        UUID voteSessionId,
        String restaurantName,
        String url,
        String address,
        Double latitude,
        Double longitude,
        Integer distanceM,
        String externalPlaceId,
        int goCount,
        int maybeCount,
        int noCount,
        LocalDateTime restaurantCreatedAt
) {
}
