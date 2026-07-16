package com.gm.client.kakao.local.dto;

public record SearchRequest(
        String menuName,
        double longitude,
        double latitude,
        int radius
)
{
}
