package com.nhnacademy.insightonauth.exception.email;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class InvalidReactiveTokenException extends BusinessException {
    public InvalidReactiveTokenException(String message) {
        super(ErrorCode.INVALID_REACTIVE_TOKEN, message);
    }
}
