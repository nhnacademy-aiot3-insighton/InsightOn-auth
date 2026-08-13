package com.nhnacademy.insightonauth.handler;

import com.nhnacademy.insightonauth.exception.BusinessException;
import com.nhnacademy.insightonauth.exception.ErrorResponse;
import com.nhnacademy.insightonauth.exception.OauthConflictResponse;
import com.nhnacademy.insightonauth.exception.OauthLinkedToOtherAccountException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // OAuth 병합 유도 — conflictingUserId를 응답에 포함
    @ExceptionHandler(OauthLinkedToOtherAccountException.class)
    public ResponseEntity<OauthConflictResponse> handleOauthLinkedToOtherAccount(
            OauthLinkedToOtherAccountException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new OauthConflictResponse(
                        e.getStatus().value(),
                        e.getMessage(),
                        e.getConflictingUserId()));
    }

    // 비즈니스 로직 실패
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getStatus().value(), e.getMessage()));
    }

    // Validate 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
    }

    // 그 외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("예상치 못한 예외 발생", e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "서버 오류가 발생했습니다."));
    }
}
