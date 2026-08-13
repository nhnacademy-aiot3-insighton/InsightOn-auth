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
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT),
    OAUTH_LINKED_TO_OTHER_ACCOUNT(HttpStatus.CONFLICT),
    INVALID_MERGE_REQUEST(HttpStatus.BAD_REQUEST),
    // 이메일 인증 코드 재전송 — 연타 방지(쿨다운)
    VERIFICATION_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS),
    // 이메일 인증 코드 재전송 — 횟수 초과 잠금
    VERIFICATION_RESEND_LOCKED(HttpStatus.LOCKED),

    // 비밀번호 재설정 메일 재전송 — 연타 방지(쿨다운)
    PASSWORD_RESET_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS),
    // 비밀번호 재설정 메일 재전송 — 횟수 초과 잠금
    PASSWORD_RESET_RESEND_LOCKED(HttpStatus.LOCKED);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
