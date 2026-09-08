package com.nhnacademy.insightonauth.exception.email;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class EmailNotFoundException extends BusinessException {
    public EmailNotFoundException(String message) {
        super(ErrorCode.EMAIL_NOT_FOUND, message);
    }
}
