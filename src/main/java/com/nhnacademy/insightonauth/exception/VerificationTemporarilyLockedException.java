package com.nhnacademy.insightonauth.exception;

public class VerificationTemporarilyLockedException extends RuntimeException {
    public VerificationTemporarilyLockedException(String message) {
        super(message);
    }
}
