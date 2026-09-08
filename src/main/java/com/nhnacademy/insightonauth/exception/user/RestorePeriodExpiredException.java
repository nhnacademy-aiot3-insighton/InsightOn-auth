package com.nhnacademy.insightonauth.exception.user;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class RestorePeriodExpiredException extends BusinessException {
    public RestorePeriodExpiredException(String message) {
        super(ErrorCode.RESTORE_PERIOD_EXPIRED, message);
    }
}
