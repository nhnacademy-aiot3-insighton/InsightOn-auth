package com.nhnacademy.insightonauth.exception;

public record ErrorResponse(
        int status,
        String message
) {
}
