package com.nhnacademy.insightonauth.exception;

public class UserCredentialsNotFoundException extends BusinessException {
    public UserCredentialsNotFoundException(String message) {
        super(ErrorCode.USER_CREDENTIALS_NOT_FOUND, message);
    }
}
