package com.nhnacademy.insightonauth.exception.email;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class PasswordResetResendTooSoonException extends BusinessException {
    public PasswordResetResendTooSoonException(String message) {
        super(ErrorCode.PASSWORD_RESET_RESEND_TOO_SOON, message);
    }
}
