package com.nhnacademy.insightonauth.exception;

public class ReactivationConflictException extends BusinessException {
    public ReactivationConflictException(String message) {
        super(ErrorCode.REACTIVATION_CONFLICT, message);
    }
}
