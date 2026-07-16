package com.gm.client.kakao.local.dto;

public record PlaceSearchRequest(
        String menuName,
        double longitude,
        double latitude,
        double radius
)
{
}
