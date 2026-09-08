package com.nhnacademy.insightonauth.exception;

public record OauthConflictResponse(
        int status,
        String message,
        Long conflictingUserId
) {

}
