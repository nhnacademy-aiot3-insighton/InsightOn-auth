package com.nhnacademy.insightonauth.exception;

public class RedisOperationException extends BusinessException {
    public RedisOperationException(String message) {
        super(ErrorCode.REDIS_OPERATION_FAILED, message);
    }
}
