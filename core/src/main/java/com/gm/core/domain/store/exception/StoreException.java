package com.gm.core.domain.store.exception;

import com.gm.core.exception.BusinessException;

public class StoreException extends BusinessException {

    public StoreException(StoreErrorCode storeErrorCode) {
        super(storeErrorCode);
    }
}
