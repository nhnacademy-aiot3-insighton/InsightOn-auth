package com.nhnacademy.insightonauth.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
    }

    public HttpStatus getStatus() { return status; }
    public ErrorCode getErrorCode() { return errorCode; }
}