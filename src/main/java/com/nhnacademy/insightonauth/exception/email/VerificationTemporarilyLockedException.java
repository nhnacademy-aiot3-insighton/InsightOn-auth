package com.nhnacademy.insightonauth.exception.email;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class VerificationTemporarilyLockedException extends BusinessException {
    public VerificationTemporarilyLockedException(String message) {
        super(ErrorCode.VERIFICATION_TEMPORARILY_LOCKED, message);
    }
}
