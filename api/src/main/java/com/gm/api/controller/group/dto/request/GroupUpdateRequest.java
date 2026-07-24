package com.gm.api.controller.group.dto.request;

import java.time.LocalTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gm.core.domain.group.model.GroupUpdate;

public record GroupUpdateRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Size(max = 500)
        String locationAddress,

        @NotNull
        @DecimalMin("-90")
        @DecimalMax("90")
        Double latitude,

        @NotNull
        @DecimalMin("-180")
        @DecimalMax("180")
        Double longitude,

        @NotNull
        @Positive
        Integer searchRadiusM,

        @NotNull
        @JsonFormat(pattern = "HH:mm")
        LocalTime recommendationTime,

        @NotNull
        @Positive
        Integer maxMemberCount
) {
    public GroupUpdate toGroupUpdate() {
        return new GroupUpdate(
                name,
                locationAddress,
                latitude,
                longitude,
                searchRadiusM,
                recommendationTime,
                maxMemberCount
        );
    }
}
