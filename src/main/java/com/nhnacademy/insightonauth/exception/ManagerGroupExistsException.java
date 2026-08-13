package com.nhnacademy.insightonauth.exception;

public class ManagerGroupExistsException extends BusinessException {
    public ManagerGroupExistsException(String message) {
        super(ErrorCode.MANAGER_GROUP_EXISTS, message);
    }
}
