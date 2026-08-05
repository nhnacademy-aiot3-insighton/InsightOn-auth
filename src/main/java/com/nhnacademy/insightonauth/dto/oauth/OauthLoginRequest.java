package com.nhnacademy.insightonauth.dto.oauth;

import jakarta.validation.constraints.NotBlank;

public record OauthLoginRequest(
        @NotBlank(message = "code는 필수입니다.")
        String code
) {}