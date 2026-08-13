package com.nhnacademy.insightonauth.exception;

public class CoreServiceUnavailableException extends BusinessException {
    public CoreServiceUnavailableException(String message) {
        super(ErrorCode.CORE_SERVICE_UNAVAILABLE, message);
    }
}
