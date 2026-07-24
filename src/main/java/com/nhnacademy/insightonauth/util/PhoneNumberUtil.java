package com.nhnacademy.insightonauth.util;

public final class PhoneNumberUtil {

    private PhoneNumberUtil() {
    }

    public static String normalize(String phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.replaceAll("[^0-9]", "");
    }
}