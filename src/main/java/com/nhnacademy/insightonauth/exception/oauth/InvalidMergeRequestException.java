package com.nhnacademy.insightonauth.exception.oauth;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;

public class InvalidMergeRequestException extends BusinessException {
    public InvalidMergeRequestException(String message) {
        super(ErrorCode.INVALID_MERGE_REQUEST, message);
    }
}
