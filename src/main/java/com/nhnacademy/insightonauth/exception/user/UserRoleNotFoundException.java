package com.nhnacademy.insightonauth.exception.user;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class UserRoleNotFoundException extends BusinessException {
    public UserRoleNotFoundException(String message) {
        super(ErrorCode.USER_ROLE_NOT_FOUND, message);
    }
}
