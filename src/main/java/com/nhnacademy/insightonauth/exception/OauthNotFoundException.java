package com.nhnacademy.insightonauth.exception;

public class OauthNotFoundException extends BusinessException {
    public OauthNotFoundException(String message) {
        super(ErrorCode.OAUTH_NOT_FOUND, message);
    }
}
