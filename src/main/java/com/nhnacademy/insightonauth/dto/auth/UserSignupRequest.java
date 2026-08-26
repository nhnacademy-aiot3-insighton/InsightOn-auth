package com.nhnacademy.insightonauth.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSignupRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 254, message = "이메일은 254자를 초과할 수 없습니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "이메일 형식이 올바르지 않습니다."
        )
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\",.<>/?]).+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 각각 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 100, message = "이름은 2자 이상 100자 이하여야 합니다.")
        String userName,

        @NotBlank(message = "전화번호는 필수입니다.")
        // 국내 전화번호 형식 검증: 0으로 시작 + 휴대폰(010~019)/서울(02)/지방 지역번호/인터넷전화(070) + 나머지 7~8자리 숫자
        @Pattern(
                regexp = "^0(1[016789]|2|31|32|33|41|42|43|44|51|52|53|54|55|61|62|63|64|70)\\d{7,8}$",
                message = "전화번호 형식이 올바르지 않습니다."
        )
        String phoneNumber,

        @NotBlank(message = "토큰은 필수입니다.")
        String token
) {
    public UserSignupRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (userName != null) {
            userName = userName.trim();
        }
        if (phoneNumber != null) {
            phoneNumber = phoneNumber.trim();
        }
    }
}