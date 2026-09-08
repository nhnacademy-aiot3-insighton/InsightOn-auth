package com.nhnacademy.insightonauth.exception.email;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class VerificationResendTooSoonException extends BusinessException {
    public VerificationResendTooSoonException(String message) {
        super(ErrorCode.VERIFICATION_RESEND_TOO_SOON, message);
    }
}
