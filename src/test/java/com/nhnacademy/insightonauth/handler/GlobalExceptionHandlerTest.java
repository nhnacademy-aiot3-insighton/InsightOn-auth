package com.nhnacademy.insightonauth.handler;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorCode;
import com.nhnacademy.insightonauth.exception.ErrorResponse;
import com.nhnacademy.insightonauth.exception.OauthConflictResponse;
import com.nhnacademy.insightonauth.exception.oauth.OauthLinkedToOtherAccountException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleOauthLinkedToOtherAccount - conflictingUserId 포함 응답")
    void handleOauthLinkedToOtherAccount() {
        OauthLinkedToOtherAccountException e =
                new OauthLinkedToOtherAccountException("다른 계정에 연동됨", 42L);

        ResponseEntity<OauthConflictResponse> response = handler.handleOauthLinkedToOtherAccount(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("다른 계정에 연동됨");
        assertThat(response.getBody().conflictingUserId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("handleBusinessException - ErrorCode의 status/message 그대로 응답")
    void handleBusinessException() {
        BusinessException e = new BusinessException(ErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다.");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("유저를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("handleValidation - 첫 필드 오류 메시지로 400 응답")
    void handleValidation() {
        FieldError fieldError = new FieldError("target", "email", "이메일 형식이 올바르지 않습니다.");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        when(e.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("이메일 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("handleValidation - 필드 오류가 없으면 기본 메시지")
    void handleValidation_noFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        when(e.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(e);

        assertThat(response.getBody().message()).isEqualTo("잘못된 요청입니다.");
    }

    @Test
    @DisplayName("handleConstraintViolation - 첫 위반 메시지로 400 응답")
    @SuppressWarnings("unchecked")
    void handleConstraintViolation() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("잘못된 값입니다.");
        ConstraintViolationException e = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("잘못된 값입니다.");
    }

    @Test
    @DisplayName("handleConstraintViolation - 위반이 없으면 기본 메시지")
    void handleConstraintViolation_empty() {
        ConstraintViolationException e = new ConstraintViolationException(Set.of());

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(e);

        assertThat(response.getBody().message()).isEqualTo("잘못된 요청입니다.");
    }

    @Test
    @DisplayName("handleUnexpected - 500 고정 메시지 응답")
    void handleUnexpected() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("서버 오류가 발생했습니다.");
    }
}
