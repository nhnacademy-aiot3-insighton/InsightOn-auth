package com.nhnacademy.insightonauth.exception.email;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class PasswordResetResendLockedException extends BusinessException {
    public PasswordResetResendLockedException(String message) {
        super(ErrorCode.PASSWORD_RESET_RESEND_LOCKED, message);
    }
}
