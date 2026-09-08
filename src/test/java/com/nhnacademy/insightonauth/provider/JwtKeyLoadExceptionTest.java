package com.nhnacademy.insightonauth.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtKeyLoadExceptionTest {

    @Test
    @DisplayName("message/cause를 그대로 보관한다")
    void constructor() {
        Exception cause = new Exception("원인");

        JwtKeyLoadException e = new JwtKeyLoadException("로딩 실패", cause);

        assertThat(e.getMessage()).isEqualTo("로딩 실패");
        assertThat(e.getCause()).isEqualTo(cause);
    }
}
