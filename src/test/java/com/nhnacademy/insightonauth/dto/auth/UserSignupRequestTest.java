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

class UserSignupRequestTest {

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

    private UserSignupRequest valid() {
        return new UserSignupRequest("test@example.com", "Abcd1234!", "홍길동", "01012345678", "verify-token");
    }

    @Test
    @DisplayName("이메일/이름/전화번호를 trim 및 정규화한다")
    void normalizesFields() {
        UserSignupRequest request = new UserSignupRequest(
                "  TEST@Example.COM  ", "Abcd1234!", "  홍길동  ", "  01012345678  ", "token");

        assertThat(request.email()).isEqualTo("test@example.com");
        assertThat(request.userName()).isEqualTo("홍길동");
        assertThat(request.phoneNumber()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("정상 입력이면 검증 통과")
    void valid_passes() {
        Set<ConstraintViolation<UserSignupRequest>> violations = validator.validate(valid());

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("이름이 1자면 검증 실패")
    void tooShortName_invalid() {
        UserSignupRequest request = new UserSignupRequest(
                "test@example.com", "Abcd1234!", "홍", "01012345678", "token");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("휴대폰 번호(010~019) 형식이면 검증 통과")
    void mobilePhoneNumber_valid() {
        UserSignupRequest request = new UserSignupRequest(
                "test@example.com", "Abcd1234!", "홍길동", "01912345678", "token");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("전화번호 형식이 틀리면 검증 실패")
    void malformedPhoneNumber_invalid() {
        UserSignupRequest request = new UserSignupRequest(
                "test@example.com", "Abcd1234!", "홍길동", "1234", "token");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("토큰이 비어있으면 검증 실패")
    void blankToken_invalid() {
        UserSignupRequest request = new UserSignupRequest(
                "test@example.com", "Abcd1234!", "홍길동", "01012345678", "");

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
