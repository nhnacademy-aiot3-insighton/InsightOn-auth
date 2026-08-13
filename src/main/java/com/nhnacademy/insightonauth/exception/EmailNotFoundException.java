package com.nhnacademy.insightonauth.exception;

public class EmailNotFoundException extends BusinessException {
    public EmailNotFoundException(String message) {
        super(ErrorCode.EMAIL_NOT_FOUND, message);
    }
}
