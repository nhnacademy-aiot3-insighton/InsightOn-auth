package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.dto.auth.UserLoginResult;
import com.nhnacademy.insightonauth.handler.GlobalExceptionHandler;
import com.nhnacademy.insightonauth.service.UserAuthenticationService;
import com.nhnacademy.insightonauth.service.UserEmailService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import com.nhnacademy.insightonauth.service.UserRoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AccountController HTTP 계약 고정 테스트 — 계정 복구 엔드포인트.
 */
@WebMvcTest(controllers = AccountController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean UserEmailService userEmailService;
    @MockitoBean UserManagementService userManagementService;
    @MockitoBean UserAuthenticationService userAuthenticationService; // HeaderAuthenticationFilter 의존성
    @MockitoBean UserRoleService userRoleService;                     // HeaderAuthenticationFilter 의존성

    @Test
    @DisplayName("POST /reactive — 200, 복구 후 로그인 응답")
    void reactive() throws Exception {
        when(userEmailService.reactive("rt-123"))
                .thenReturn(UserLoginResult.success("acc", "ref"));

        mvc.perform(post("/api/v1/auth/reactive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reactiveToken": "rt-123" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /reactive — 토큰 누락 400")
    void reactive_validation() throws Exception {
        mvc.perform(post("/api/v1/auth/reactive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reactiveToken": "" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /reactivate/email-verify-request — 204")
    void reactivateRequest() throws Exception {
        mvc.perform(post("/api/v1/auth/reactivate/email-verify-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "user@test.com" }
                                """))
                .andExpect(status().isNoContent());

        verify(userEmailService).reactivateRequest("user@test.com");
    }

    @Test
    @DisplayName("POST /reactivate/email-verify-confirm — 200, 복구 후 로그인 응답")
    void reactivateConfirm() throws Exception {
        when(userEmailService.reactivateConfirm("user@test.com", "123456"))
                .thenReturn(UserLoginResult.success("acc", "ref"));

        mvc.perform(post("/api/v1/auth/reactivate/email-verify-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "user@test.com", "code": "123456" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /find-email — 200, 마스킹된 이메일 반환")
    void findEmail() throws Exception {
        when(userManagementService.findMaskedEmail("홍길동", "010-1234-5678"))
                .thenReturn("ho****@test.com");

        mvc.perform(post("/api/v1/auth/find-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "userName": "홍길동", "phoneNumber": "010-1234-5678" }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("ho****@test.com"));
    }

    @Test
    @DisplayName("POST /find-email — 이름 1글자 400")
    void findEmail_validation() throws Exception {
        mvc.perform(post("/api/v1/auth/find-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "userName": "김", "phoneNumber": "01012345678" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /password/reset-request — 204")
    void passwordResetRequest() throws Exception {
        mvc.perform(post("/api/v1/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "user@test.com" }
                                """))
                .andExpect(status().isNoContent());

        verify(userEmailService).passwordResetRequest("user@test.com");
    }

    @Test
    @DisplayName("POST /password/reset-confirm — 200")
    void passwordResetConfirm() throws Exception {
        mvc.perform(post("/api/v1/auth/password/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "password": "Abcd1234!", "token": "reset-token" }
                                """))
                .andExpect(status().isOk());

        verify(userEmailService).passwordResetConfirm("reset-token", "Abcd1234!");
    }

    @Test
    @DisplayName("POST /password/reset-confirm — 비밀번호 규칙 위반 400")
    void passwordResetConfirm_validation() throws Exception {
        mvc.perform(post("/api/v1/auth/password/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "password": "weak", "token": "reset-token" }
                                """))
                .andExpect(status().isBadRequest());
    }
}
