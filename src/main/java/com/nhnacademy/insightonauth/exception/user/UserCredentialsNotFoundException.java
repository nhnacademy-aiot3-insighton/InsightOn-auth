package com.nhnacademy.insightonauth.exception.user;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class UserCredentialsNotFoundException extends BusinessException {
    public UserCredentialsNotFoundException(String message) {
        super(ErrorCode.USER_CREDENTIALS_NOT_FOUND, message);
    }
}
