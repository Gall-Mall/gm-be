package com.gm.client.kakao.local.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResponseWrapper(
        List<SearchResponse> documents
) {
}
