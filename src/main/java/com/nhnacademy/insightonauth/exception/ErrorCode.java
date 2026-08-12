package com.nhnacademy.insightonauth.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    CORE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT),
    DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND),
    EMAIL_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    INVALID_REACTIVE_TOKEN(HttpStatus.BAD_REQUEST),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED),
    INVALID_USER(HttpStatus.FORBIDDEN),
    INVALID_USER_STATUS(HttpStatus.CONFLICT),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST),
    INVALID_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST),
    LAST_LOGIN_METHOD(HttpStatus.CONFLICT),
    LOGIN_TEMPORARILY_LOCKED(HttpStatus.LOCKED),
    MANAGER_GROUP_EXISTS(HttpStatus.CONFLICT),
    OAUTH_ALREADY_LINKED(HttpStatus.CONFLICT),
    OAUTH_NOT_FOUND(HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND),
    RESTORE_PERIOD_EXPIRED(HttpStatus.GONE),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST),
    USER_CREDENTIALS_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND),
    VERIFICATION_TEMPORARILY_LOCKED(HttpStatus.LOCKED),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
