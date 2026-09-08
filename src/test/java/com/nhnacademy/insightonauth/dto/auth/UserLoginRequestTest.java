package com.nhnacademy.insightonauth.dto.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserLoginRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    @DisplayName("이메일을 trim하고 소문자로 정규화한다")
    void normalizesEmail() {
        UserLoginRequest request = new UserLoginRequest("  TEST@Example.COM  ", "Abcd1234!");

        assertThat(request.email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("email이 null이면 정규화 없이 그대로 null")
    void nullEmail_notNormalized() {
        UserLoginRequest request = new UserLoginRequest(null, "Abcd1234!");

        assertThat(request.email()).isNull();
    }

    @Test
    @DisplayName("정상 입력이면 검증 통과")
    void valid() {
        UserLoginRequest request = new UserLoginRequest("test@example.com", "Abcd1234!");

        Set<ConstraintViolation<UserLoginRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("이메일이 빈 값이면 검증 실패")
    void blankEmail_invalid() {
        UserLoginRequest request = new UserLoginRequest("", "Abcd1234!");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("이메일 형식이 틀리면 검증 실패")
    void malformedEmail_invalid() {
        UserLoginRequest request = new UserLoginRequest("not-an-email", "Abcd1234!");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 검증 실패")
    void shortPassword_invalid() {
        UserLoginRequest request = new UserLoginRequest("test@example.com", "a1!");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("비밀번호에 특수문자가 없으면 검증 실패")
    void passwordWithoutSpecialChar_invalid() {
        UserLoginRequest request = new UserLoginRequest("test@example.com", "abcd1234");

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
