package com.nhnacademy.insightonauth.exception.email;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class VerificationResendLockedException extends BusinessException {
    public VerificationResendLockedException(String message) {
        super(ErrorCode.VERIFICATION_RESEND_LOCKED, message);
    }
}
