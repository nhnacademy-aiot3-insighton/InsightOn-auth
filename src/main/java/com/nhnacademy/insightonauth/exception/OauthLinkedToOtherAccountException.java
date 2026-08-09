package com.nhnacademy.insightonauth.exception;

public class OauthLinkedToOtherAccountException extends BusinessException {
    public OauthLinkedToOtherAccountException(String message, Long conflictingUserId) {
        super(ErrorCode.OAUTH_LINKED_TO_OTHER_ACCOUNT, message);
    }
}
