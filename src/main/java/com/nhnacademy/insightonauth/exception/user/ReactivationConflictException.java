package com.nhnacademy.insightonauth.exception.user;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class ReactivationConflictException extends BusinessException {
    public ReactivationConflictException(String message) {
        super(ErrorCode.REACTIVATION_CONFLICT, message);
    }
}
