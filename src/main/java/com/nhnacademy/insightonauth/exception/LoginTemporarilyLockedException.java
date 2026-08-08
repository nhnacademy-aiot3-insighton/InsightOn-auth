package com.nhnacademy.insightonauth.exception;

public class LoginTemporarilyLockedException extends BusinessException {
    public LoginTemporarilyLockedException(String message) {
        super(ErrorCode.LOGIN_TEMPORARILY_LOCKED, message);
    }
}
