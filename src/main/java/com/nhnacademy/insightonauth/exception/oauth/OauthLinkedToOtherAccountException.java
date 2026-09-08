package com.nhnacademy.insightonauth.exception.oauth;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

import lombok.Getter;

@Getter
public class OauthLinkedToOtherAccountException extends BusinessException {
    private final Long conflictingUserId;

    public OauthLinkedToOtherAccountException(String message, Long conflictingUserId) {
        super(ErrorCode.OAUTH_LINKED_TO_OTHER_ACCOUNT, message);
        this.conflictingUserId = conflictingUserId;
    }

}
