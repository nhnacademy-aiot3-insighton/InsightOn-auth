package com.nhnacademy.insightonauth.exception;

public class VerificationResendTooSoonException extends BusinessException {
    public VerificationResendTooSoonException(String message) {
        super(ErrorCode.VERIFICATION_RESEND_TOO_SOON, message);
    }
}
