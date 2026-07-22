package com.gm.core.domain.user_setting.exception;

import com.gm.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSettingErrorCode implements ErrorCode {
    CATEGORY_PREFERENCE_CONFLICT(400, "USER_SETTING-001", "선호 카테고리와 비선호 카테고리가 중복되었습니다."),
    MENU_PREFERENCE_CONFLICT(400, "USER_SETTING-002", "선호 메뉴와 비선호 메뉴가 중복되었습니다."),
    CATEGORY_NOT_FOUND(404, "USER_SETTING-003", "카테고리를 찾을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}
