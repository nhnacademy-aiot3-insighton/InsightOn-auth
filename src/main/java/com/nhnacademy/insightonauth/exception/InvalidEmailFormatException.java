package com.nhnacademy.insightonauth.exception;

public class InvalidEmailFormatException extends BusinessException {
    public InvalidEmailFormatException(String message) {
        super(ErrorCode.INVALID_EMAIL_FORMAT, message);
    }
}
