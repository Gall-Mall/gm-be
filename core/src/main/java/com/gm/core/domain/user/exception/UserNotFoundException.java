package com.gm.core.domain.user.exception;

public class UserNotFoundException extends RuntimeException {

    private static final String MESSAGE = "존재하지 않는 회원입니다.";

    public UserNotFoundException() {
        super(MESSAGE);
    }
}