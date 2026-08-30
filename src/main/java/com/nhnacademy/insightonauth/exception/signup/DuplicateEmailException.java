package com.nhnacademy.insightonauth.exception.signup;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class DuplicateEmailException extends BusinessException {
    public DuplicateEmailException(String message) {
        super(ErrorCode.DUPLICATE_EMAIL, message);
    }
}
