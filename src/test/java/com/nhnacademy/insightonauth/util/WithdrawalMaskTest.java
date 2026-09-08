package com.nhnacademy.insightonauth.util;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WithdrawalMaskTest {

    @Test
    @DisplayName("접미사가 붙은 값은 masked로 판별하고 원본을 복원")
    void masked() {
        String suffixed = "test@test.com;" + UuidCreator.getTimeOrderedEpoch();

        assertThat(WithdrawalMask.isMasked(suffixed)).isTrue();
        assertThat(WithdrawalMask.strip(suffixed)).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("원본 값에 ';'가 있어도 접미사가 없으면 masked 아님")
    void notMasked_withSemicolon() {
        assertThat(WithdrawalMask.isMasked("a;b@test.com")).isFalse();
        assertThat(WithdrawalMask.strip("a;b@test.com")).isEqualTo("a;b@test.com");
    }

    @Test
    @DisplayName("원본에 ';'가 있고 접미사도 붙은 경우 접미사만 제거")
    void masked_originalHasSemicolon() {
        String suffixed = "a;b@test.com;" + UuidCreator.getTimeOrderedEpoch();

        assertThat(WithdrawalMask.isMasked(suffixed)).isTrue();
        assertThat(WithdrawalMask.strip(suffixed)).isEqualTo("a;b@test.com");
    }

    @Test
    @DisplayName("';'로 끝나지만 UUID 형식이 아니면 masked 아님")
    void notMasked_semicolonButNotUuid() {
        assertThat(WithdrawalMask.isMasked("pid;12345")).isFalse();
    }

    @Test
    @DisplayName("null은 masked 아니고 strip 결과도 null")
    void nullSafe() {
        assertThat(WithdrawalMask.isMasked(null)).isFalse();
        assertThat(WithdrawalMask.strip(null)).isNull();
    }
}
