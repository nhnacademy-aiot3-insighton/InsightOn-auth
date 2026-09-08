package com.nhnacademy.insightonauth.exception.signup;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class DuplicatePhoneNumberException extends BusinessException {
    public DuplicatePhoneNumberException(String message) {
        super(ErrorCode.DUPLICATE_PHONE_NUMBER, message);
    }
}
