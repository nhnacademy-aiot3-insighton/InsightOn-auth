package com.nhnacademy.insightonauth.exception;

public class LoginTemporarilyLockedException extends RuntimeException {
    public LoginTemporarilyLockedException(String message) {
        super(message);
    }
}
