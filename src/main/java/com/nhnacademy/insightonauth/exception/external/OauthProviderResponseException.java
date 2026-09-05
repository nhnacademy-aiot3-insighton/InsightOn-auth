package com.nhnacademy.insightonauth.exception.external;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class OauthProviderResponseException extends BusinessException {
    public OauthProviderResponseException(String message) {
        super(ErrorCode.OAUTH_PROVIDER_RESPONSE_INVALID, message);
    }
}
