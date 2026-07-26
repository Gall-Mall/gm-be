package com.gm.core.domain.store.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.gm.core.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum StoreErrorCode implements ErrorCode {

    RESTAURANT_NOT_FOUND(404, "STORE-001", "식당을 찾을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}
