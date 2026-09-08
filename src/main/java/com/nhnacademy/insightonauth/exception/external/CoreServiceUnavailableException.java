package com.nhnacademy.insightonauth.exception.external;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class CoreServiceUnavailableException extends BusinessException {
    public CoreServiceUnavailableException(String message) {
        super(ErrorCode.CORE_SERVICE_UNAVAILABLE, message);
    }
}
