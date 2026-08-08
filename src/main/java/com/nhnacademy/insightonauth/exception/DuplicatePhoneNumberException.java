package com.nhnacademy.insightonauth.exception;

public class DuplicatePhoneNumberException extends BusinessException {
    public DuplicatePhoneNumberException(String message) {
        super(ErrorCode.DUPLICATE_PHONE_NUMBER, message);
    }
}
