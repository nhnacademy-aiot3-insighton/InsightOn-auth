package com.nhnacademy.insightonauth.exception;

public class UserRoleNotFoundException extends BusinessException {
    public UserRoleNotFoundException(String message) {
        super(ErrorCode.USER_ROLE_NOT_FOUND, message);
    }
}
