package com.nhnacademy.insightonauth.exception.external;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class RedisOperationException extends BusinessException {
    public RedisOperationException(String message) {
        super(ErrorCode.REDIS_OPERATION_FAILED, message);
    }
}
