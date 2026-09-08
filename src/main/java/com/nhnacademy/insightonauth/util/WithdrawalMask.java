package com.nhnacademy.insightonauth.util;

import java.util.regex.Pattern;

/**
 * 탈퇴 시 유니크 컬럼(email, phone_number, provider_user_id)에 붙이는 마스킹 접미사 처리.
 * 접미사는 {@code ";" + UUID} 형식이며, 원본 값에 ";" 가 들어 있어도 오판하지 않도록
 * 문자열 끝의 정확한 접미사 패턴만 인식한다.
 */
public final class WithdrawalMask {

    private static final Pattern SUFFIX = Pattern.compile(
            ";[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private WithdrawalMask() {
    }

    public static boolean isMasked(String value) {
        return value != null && SUFFIX.matcher(value).find();
    }

    /** 마스킹 접미사를 뗀 원본 값 (마스킹 안 됐으면 그대로, null이면 null). */
    public static String strip(String value) {
        return value == null ? null : SUFFIX.matcher(value).replaceFirst("");
    }
}
