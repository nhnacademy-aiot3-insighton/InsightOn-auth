package com.nhnacademy.insightonauth.exception;

public class DuplicateEmailException extends BusinessException {
    public DuplicateEmailException(String message) {
        super(ErrorCode.DUPLICATE_EMAIL, message);
    }
}
