package com.nhnacademy.insightonauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\",.<>/?]).+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 각각 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "토큰은 필수입니다.")
        String token
) {
}
