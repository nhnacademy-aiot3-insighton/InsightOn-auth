package com.nhnacademy.insightonauth.exception;

public class PasswordResetResendLockedException extends BusinessException {
    public PasswordResetResendLockedException(String message) {
        super(ErrorCode.PASSWORD_RESET_RESEND_LOCKED, message);
    }
}
