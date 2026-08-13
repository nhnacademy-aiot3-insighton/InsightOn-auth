package com.nhnacademy.insightonauth.exception;

public class VerificationResendLockedException extends BusinessException {
    public VerificationResendLockedException(String message) {
        super(ErrorCode.VERIFICATION_RESEND_LOCKED, message);
    }
}
