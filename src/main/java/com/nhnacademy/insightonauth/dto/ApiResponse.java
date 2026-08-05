package com.nhnacademy.insightonauth.dto;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    // 성공 응답용 생성자
    public ApiResponse(T data) {
        this.success = true;
        this.data = data;
        this.error = null;
    }

    public ApiResponse(String code, String message) {
        this.success = false;
        this.data = null;
        this.error = new ErrorResponse(code, message);
    }
}
