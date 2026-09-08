package com.nhnacademy.insightonauth.exception.oauth;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class LastLoginMethodException extends BusinessException {
    public LastLoginMethodException(String message) {
        super(ErrorCode.LAST_LOGIN_METHOD, message);
    }
}
