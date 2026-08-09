package com.nhnacademy.insightonauth.exception;

public class InvalidMergeRequestException extends BusinessException {
    public InvalidMergeRequestException(String message) {
        super(ErrorCode.INVALID_MERGE_REQUEST, message);
    }
}
