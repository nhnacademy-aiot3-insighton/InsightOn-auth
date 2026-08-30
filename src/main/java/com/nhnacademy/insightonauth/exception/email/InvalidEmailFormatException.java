package com.nhnacademy.insightonauth.exception.email;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class InvalidEmailFormatException extends BusinessException {
    public InvalidEmailFormatException(String message) {
        super(ErrorCode.INVALID_EMAIL_FORMAT, message);
    }
}
