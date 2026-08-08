package com.nhnacademy.insightonauth.exception;

public class LastLoginMethodException extends BusinessException {
    public LastLoginMethodException(String message) {
        super(ErrorCode.LAST_LOGIN_METHOD, message);
    }
}
