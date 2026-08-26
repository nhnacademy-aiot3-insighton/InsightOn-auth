package com.nhnacademy.insightonauth.exception;

public class SameAsOldPasswordException extends BusinessException {
    public SameAsOldPasswordException(String message) {
        super(ErrorCode.SAME_AS_OLD_PASSWORD, message);
    }
}
