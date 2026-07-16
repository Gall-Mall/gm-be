package com.gm.client.kakao.local.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gm.client.kakao.local.dto.PlaceSearchResponse;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResponseWrapper(
        List<PlaceSearchResponse> documents
) {
}
