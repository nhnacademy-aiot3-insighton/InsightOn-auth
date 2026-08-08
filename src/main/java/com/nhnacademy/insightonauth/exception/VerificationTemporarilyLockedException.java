package com.nhnacademy.insightonauth.exception;

public class VerificationTemporarilyLockedException extends BusinessException {
    public VerificationTemporarilyLockedException(String message) {
        super(ErrorCode.VERIFICATION_TEMPORARILY_LOCKED, message);
    }
}
