package com.nhnacademy.insightonauth.exception.user;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class InvalidUserRoleException extends BusinessException {
    public InvalidUserRoleException(String message) {
        super(ErrorCode.INVALID_USER_ROLE, message);
    }
}
