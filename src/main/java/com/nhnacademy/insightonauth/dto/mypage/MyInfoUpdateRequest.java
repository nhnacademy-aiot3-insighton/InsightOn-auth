package com.nhnacademy.insightonauth.dto.mypage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MyInfoUpdateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.")
        String name,

        @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다.")
        @Pattern(regexp = "^[0-9-]*$", message = "전화번호 형식이 올바르지 않습니다.")
        String phoneNumber
) {
}
