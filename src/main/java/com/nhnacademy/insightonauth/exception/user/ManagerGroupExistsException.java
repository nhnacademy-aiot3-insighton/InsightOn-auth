package com.nhnacademy.insightonauth.exception.user;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class ManagerGroupExistsException extends BusinessException {
    public ManagerGroupExistsException(String message) {
        super(ErrorCode.MANAGER_GROUP_EXISTS, message);
    }
}
