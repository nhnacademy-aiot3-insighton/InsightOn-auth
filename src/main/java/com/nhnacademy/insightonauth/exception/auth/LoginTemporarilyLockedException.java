package com.nhnacademy.insightonauth.exception.auth;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class LoginTemporarilyLockedException extends BusinessException {
    public LoginTemporarilyLockedException(String message) {
        super(ErrorCode.LOGIN_TEMPORARILY_LOCKED, message);
    }
}
