package com.nhnacademy.insightonauth.exception.oauth;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class OauthAlreadyLinkedException extends BusinessException {
    public OauthAlreadyLinkedException(String message) {
        super(ErrorCode.OAUTH_ALREADY_LINKED, message);
    }
}
