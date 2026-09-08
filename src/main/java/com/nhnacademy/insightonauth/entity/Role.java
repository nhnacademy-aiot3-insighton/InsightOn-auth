package com.nhnacademy.insightonauth.entity;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN("관리자", true, false),
    MEMBER("일반회원", false, true);

    /** 화면 표시명 */
    private final String label;
    /** true 면 다른 권한과 함께 가질 수 없음 (단독 보유) */
    private final boolean exclusive;
    /** true 면 배타 역할이 없을 때 항상 보유하는 기본 권한 */
    private final boolean base;

    Role(String label, boolean exclusive, boolean base) {
        this.label = label;
        this.exclusive = exclusive;
        this.base = base;
    }
}
