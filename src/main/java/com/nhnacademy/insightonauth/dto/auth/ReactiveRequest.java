package com.nhnacademy.insightonauth.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record ReactiveRequest(
        @NotBlank(message = "복구 토큰은 필수입니다.")
        String reactiveToken
) {
}
