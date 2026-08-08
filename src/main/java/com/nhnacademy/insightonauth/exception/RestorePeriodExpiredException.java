package com.nhnacademy.insightonauth.exception;

public class RestorePeriodExpiredException extends BusinessException {
    public RestorePeriodExpiredException(String message) {
        super(ErrorCode.RESTORE_PERIOD_EXPIRED, message);
    }
}
