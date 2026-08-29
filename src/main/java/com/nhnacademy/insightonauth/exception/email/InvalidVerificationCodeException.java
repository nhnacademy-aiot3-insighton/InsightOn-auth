package com.nhnacademy.insightonauth.exception.email;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class InvalidVerificationCodeException extends BusinessException {
    public InvalidVerificationCodeException(String message) {
        super(ErrorCode.INVALID_VERIFICATION_CODE, message);
    }
}
