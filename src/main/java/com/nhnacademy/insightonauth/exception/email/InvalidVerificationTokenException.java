package com.nhnacademy.insightonauth.exception.email;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class InvalidVerificationTokenException extends BusinessException {
    public InvalidVerificationTokenException(String message) {
        super(ErrorCode.INVALID_VERIFICATION_TOKEN, message);
    }
}
