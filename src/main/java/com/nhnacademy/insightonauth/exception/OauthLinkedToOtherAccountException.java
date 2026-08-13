package com.nhnacademy.insightonauth.exception;

import lombok.Getter;

@Getter
public class OauthLinkedToOtherAccountException extends BusinessException {
    private final Long conflictingUserId;

    public OauthLinkedToOtherAccountException(String message, Long conflictingUserId) {
        super(ErrorCode.OAUTH_LINKED_TO_OTHER_ACCOUNT, message);
        this.conflictingUserId = conflictingUserId;
    }

}
