package com.nhnacademy.insightonauth.exception;

public class InvalidCredentialsException extends BusinessException {
    public InvalidCredentialsException(String message) {
        super(ErrorCode.INVALID_CREDENTIALS, message);
    }
}
