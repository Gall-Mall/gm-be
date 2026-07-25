package com.gm.api.controller.user.dto.response;

import java.util.List;
import java.util.UUID;

import com.gm.core.domain.user.model.UserSetting;

/**
 * 저장된 회원 설정을 반환한다.
 */
public record UserSettingResponse(
        List<UUID> allergenIds,
        List<UUID> preferredMenuIds,
        List<UUID> excludedMenuIds,
        List<UUID> preferredCategoryIds,
        List<UUID> excludedCategoryIds,
        String allergenText,
        String preferredText,
        String excludedText
) {

    public static UserSettingResponse from(UserSetting userSetting) {
        return new UserSettingResponse(
                userSetting.allergenIds(),
                userSetting.preferredMenuIds(),
                userSetting.excludedMenuIds(),
                userSetting.preferredCategoryIds(),
                userSetting.excludedCategoryIds(),
                userSetting.allergenText(),
                userSetting.preferredText(),
                userSetting.excludedText()
        );
    }
}
