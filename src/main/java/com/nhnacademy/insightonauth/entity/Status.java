package com.nhnacademy.insightonauth.entity;

import lombok.Getter;

@Getter
public enum Status {
    ACTIVE(true, "활성", null, null),     // 활동 계정
    SLEEP(false, "휴면", "휴면 계정입니다. 재인증이 필요합니다.", "USER_SLEEP"),      // 휴면 계정
    WITHDRAW(false, "탈퇴", "탈퇴된 계정입니다.", "USER_WITHDRAW"),   // 탈퇴 계정
    BLOCK(false, "정지", "이용이 제한된 계정입니다.", "USER_BLOCK");      // 정지 계정

    private final boolean loginable;
    private final String label;
    private final String message;
    private final String errorCode;

    Status(boolean loginable, String label, String message, String errorCode) {
        this.loginable = loginable;
        this.label = label;
        this.message = message;
        this.errorCode = errorCode;
    }

}
