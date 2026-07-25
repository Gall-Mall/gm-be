package com.gm.api.controller.user.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.gm.core.domain.user.model.UserSetting;

/**
 * 회원의 알레르기·메뉴·카테고리 선호와 자유 입력 설정을 전달한다.
 */
public record UserSettingRequest(
        @NotNull
        List<@NotNull UUID> allergenIds,

        @NotNull
        List<@NotNull UUID> preferredMenuIds,

        @NotNull
        List<@NotNull UUID> excludedMenuIds,

        @NotNull
        List<@NotNull UUID> preferredCategoryIds,

        @NotNull
        List<@NotNull UUID> excludedCategoryIds,

        @Size(max = 500)
        String allergenText,

        @Size(max = 500)
        String preferredText,

        @Size(max = 500)
        String excludedText
) {

    public UserSetting toDomain() {
        return new UserSetting(
                allergenIds,
                preferredMenuIds,
                excludedMenuIds,
                preferredCategoryIds,
                excludedCategoryIds,
                allergenText,
                preferredText,
                excludedText
        );
    }
}
