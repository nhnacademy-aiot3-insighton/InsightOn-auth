package com.nhnacademy.insightonauth.exception;

public class PasswordResetResendTooSoonException extends BusinessException {
    public PasswordResetResendTooSoonException(String message) {
        super(ErrorCode.PASSWORD_RESET_RESEND_TOO_SOON, message);
    }
}
