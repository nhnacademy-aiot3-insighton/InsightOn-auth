package com.nhnacademy.insightonauth.exception.user;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class SameAsOldPasswordException extends BusinessException {
    public SameAsOldPasswordException(String message) {
        super(ErrorCode.SAME_AS_OLD_PASSWORD, message);
    }
}
