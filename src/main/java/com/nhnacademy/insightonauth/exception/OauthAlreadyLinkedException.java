package com.nhnacademy.insightonauth.exception;

public class OauthAlreadyLinkedException extends BusinessException {
    public OauthAlreadyLinkedException(String message) {
        super(ErrorCode.OAUTH_ALREADY_LINKED, message);
    }
}
