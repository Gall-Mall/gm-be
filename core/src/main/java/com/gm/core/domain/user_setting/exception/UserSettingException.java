package com.gm.core.domain.user_setting.exception;

import com.gm.core.exception.BusinessException;

public class UserSettingException extends BusinessException {

    public UserSettingException(UserSettingErrorCode userSettingErrorCode) {
        super(userSettingErrorCode);
    }
}
