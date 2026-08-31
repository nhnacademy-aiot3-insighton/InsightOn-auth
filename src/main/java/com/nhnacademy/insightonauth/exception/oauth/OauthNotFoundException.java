package com.nhnacademy.insightonauth.exception.oauth;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class OauthNotFoundException extends BusinessException {
    public OauthNotFoundException(String message) {
        super(ErrorCode.OAUTH_NOT_FOUND, message);
    }
}
