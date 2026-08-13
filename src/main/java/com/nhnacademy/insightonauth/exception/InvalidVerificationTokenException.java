package com.nhnacademy.insightonauth.exception;

public class InvalidVerificationTokenException extends BusinessException {
    public InvalidVerificationTokenException(String message) {
        super(ErrorCode.INVALID_VERIFICATION_TOKEN, message);
    }
}
