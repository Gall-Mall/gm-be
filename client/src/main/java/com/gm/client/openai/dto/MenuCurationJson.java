package com.gm.client.openai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.gm.core.domain.recommendation.model.CuratedMenu;

/**
 * AI가 JSON mode로 반환하는 메뉴 큐레이션 결과의 역직렬화 전용 DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MenuCurationJson(
        @JsonProperty("menus") List<Item> menus
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("name") String name,
            @JsonProperty("reason") String reason
    ) {
    }

    /** null을 걸러 도메인 모델 목록으로 변환한다. */
    public List<CuratedMenu> toDomain() {
        if (menus == null) {
            return List.of();
        }
        return menus.stream()
                .filter(item -> item != null && item.name() != null)
                .map(item -> new CuratedMenu(item.name(), item.reason()))
                .toList();
    }
}
