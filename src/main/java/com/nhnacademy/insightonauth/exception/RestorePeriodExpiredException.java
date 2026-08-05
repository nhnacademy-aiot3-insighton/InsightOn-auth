package com.nhnacademy.insightonauth.exception;

public class RestorePeriodExpiredException extends RuntimeException {
    public RestorePeriodExpiredException(String message) {
        super(message);
    }
}
