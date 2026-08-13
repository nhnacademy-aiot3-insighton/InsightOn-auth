package com.nhnacademy.insightonauth.exception;

public class RefreshTokenNotFoundException extends BusinessException {
    public RefreshTokenNotFoundException(String message) {
        super(ErrorCode.REFRESH_TOKEN_NOT_FOUND, message);
    }
}
