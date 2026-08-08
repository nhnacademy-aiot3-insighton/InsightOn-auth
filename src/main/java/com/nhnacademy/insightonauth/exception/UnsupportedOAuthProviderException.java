package com.nhnacademy.insightonauth.exception;

public class UnsupportedOAuthProviderException extends BusinessException {
    public UnsupportedOAuthProviderException(String message) {
        super(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER, "지원하지 않는 OAuth 제공자입니다: " + message);
    }
}
