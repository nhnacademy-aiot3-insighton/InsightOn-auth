package com.nhnacademy.insightonauth.exception.user;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

/** 알 수 없는 회원 상태 문자열이 파라미터로 들어온 경우 (Status enum 에 없는 값). */
public class InvalidUserStatusValueException extends BusinessException {
    public InvalidUserStatusValueException(String message) {
        super(ErrorCode.INVALID_USER_STATUS_VALUE, message);
    }
}
