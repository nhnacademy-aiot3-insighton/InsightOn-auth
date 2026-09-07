package com.nhnacademy.insightonauth.exception.user;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class SelfTargetNotAllowedException extends BusinessException {
    public SelfTargetNotAllowedException(String message) {
        super(ErrorCode.SELF_TARGET_NOT_ALLOWED, message);
    }
}
