package com.nhnacademy.insightonauth.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberUtilTest {

    @Test
    @DisplayName("숫자만 있으면 그대로 반환")
    void normalize_digitsOnly() {
        assertThat(PhoneNumberUtil.normalize("01012345678")).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("대시가 있으면 제거")
    void normalize_withDashes() {
        assertThat(PhoneNumberUtil.normalize("010-1234-5678")).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("공백·괄호 등 숫자가 아닌 문자는 전부 제거")
    void normalize_withOtherNonDigits() {
        assertThat(PhoneNumberUtil.normalize("(010) 1234 5678")).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("null이면 null 반환")
    void normalize_null() {
        assertThat(PhoneNumberUtil.normalize(null)).isNull();
    }

    @Test
    @DisplayName("숫자가 하나도 없으면 빈 문자열")
    void normalize_noDigits() {
        assertThat(PhoneNumberUtil.normalize("abc-def")).isEmpty();
    }
}
