package com.gm.core.domain.user.exception;

/**
 * 사용자를 찾을 수 없는 경우 발생한다.
 */
public class UserNotFoundException extends UserException {

    public UserNotFoundException() { super(UserErrorCode.USER_NOT_FOUND); }
}