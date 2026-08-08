package com.nhnacademy.insightonauth.exception;

public class InvalidUserStatusException extends BusinessException {
    public InvalidUserStatusException(String message) {
        super(ErrorCode.INVALID_USER_STATUS, message);
    }
}
