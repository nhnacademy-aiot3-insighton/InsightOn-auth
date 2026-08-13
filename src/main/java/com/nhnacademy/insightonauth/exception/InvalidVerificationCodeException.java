package com.nhnacademy.insightonauth.exception;

public class InvalidVerificationCodeException extends BusinessException {
    public InvalidVerificationCodeException(String message) {
        super(ErrorCode.INVALID_VERIFICATION_CODE, message);
    }
}
