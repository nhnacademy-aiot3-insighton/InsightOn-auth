package com.nhnacademy.insightonauth.exception;

public class InvalidRefreshTokenException extends BusinessException {
    public InvalidRefreshTokenException(String message) {
        super(ErrorCode.INVALID_REFRESH_TOKEN, message);
    }
}
