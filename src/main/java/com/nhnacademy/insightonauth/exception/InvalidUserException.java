package com.nhnacademy.insightonauth.exception;

public class InvalidUserException extends BusinessException {
    public InvalidUserException(String message) {
        super(ErrorCode.INVALID_USER, message);
    }
}
