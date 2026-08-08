package com.nhnacademy.insightonauth.exception;

public class InvalidReactiveTokenException extends BusinessException {
    public InvalidReactiveTokenException(String message) {
        super(ErrorCode.INVALID_REACTIVE_TOKEN, message);
    }
}
