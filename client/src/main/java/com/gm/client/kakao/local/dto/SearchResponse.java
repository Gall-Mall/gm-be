package com.gm.client.kakao.local.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResponse (
        @JsonProperty(value = "address_name")
        String addressName,
        @JsonProperty(value = "category_group_name")
        String categoryGroupName,
        int distance,
        String phone,
        @JsonProperty(value = "place_name")
        String placeName,
        @JsonProperty(value = "place_url")
        String placeUrl,
        @JsonProperty(value = "y")
        double latitude,
        @JsonProperty(value = "x")
        double longitude

)
{
}

/*
"address_name": "서울 강남구 역삼동 818-4",
"category_group_code": "FD6",
"category_group_name": "음식점",
"category_name": "음식점 > 술집",
"distance": "231",
"id": "1325086783",
"phone": "010-7271-7099",
"place_name": "주도락 강남점",
"place_url": "http://place.map.kakao.com/1325086783",
"road_address_name": "서울 강남구 강남대로96길 18",
"x": "127.028468000132",
"y": "37.4998684153739"
 */
