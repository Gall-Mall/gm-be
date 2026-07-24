package com.gm.api.controller.store.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.gm.core.domain.store.model.Coordinate;

/**
 * 주변 음식점 검색 요청
 */
public record StoreSearchRequest(
        @NotNull
        UUID voteSessionId,

        @NotBlank
        String keyword,

        @NotNull
        @DecimalMin("-180")
        @DecimalMax("180")
        Double longitude,

        @NotNull
        @DecimalMin("-90")
        @DecimalMax("90")
        Double latitude,

        @NotNull
        @Positive
        @Max(10_000)
        Integer radiusM
) {
    public Coordinate toCoordinate() {
        return new Coordinate(longitude, latitude);
    }
}
