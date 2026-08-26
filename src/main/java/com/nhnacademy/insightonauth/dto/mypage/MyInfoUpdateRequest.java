package com.nhnacademy.insightonauth.dto.mypage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MyInfoUpdateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.")
        String name,

        @NotBlank(message = "전화번호는 필수입니다.")
        // 국내 전화번호 형식 검증: 0으로 시작 + 휴대폰(010~019)/서울(02)/지방(031~065)/인터넷전화(070) 앞자리 + 나머지 7~8자리 숫자
        @Pattern(
                regexp = "^0(1[016789]|2|[3-6][1-5]|70)\\d{7,8}$",
                message = "전화번호 형식이 올바르지 않습니다."
        )
        String phoneNumber
) {
}
