package com.nhnacademy.insightonauth.exception.user;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class InvalidUserStatusException extends BusinessException {
    public InvalidUserStatusException(String message) {
        super(ErrorCode.INVALID_USER_STATUS, message);
    }
}
