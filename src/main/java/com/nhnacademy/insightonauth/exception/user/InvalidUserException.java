package com.nhnacademy.insightonauth.exception.user;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class InvalidUserException extends BusinessException {
    public InvalidUserException(String message) {
        super(ErrorCode.INVALID_USER, message);
    }
}
