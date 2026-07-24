package com.nhnacademy.insightonauth.exception;

public class DuplicatePhoneNumberException extends RuntimeException {
    public DuplicatePhoneNumberException(String message) {
        super(message);
    }
}
