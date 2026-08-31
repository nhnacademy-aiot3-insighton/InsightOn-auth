package com.nhnacademy.insightonauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResult;
import com.nhnacademy.insightonauth.dto.auth.UserSignupResponse;
import com.nhnacademy.insightonauth.dto.auth.TokenRefreshResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.handler.GlobalExceptionHandler;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.service.UserAuthenticationService;
import com.nhnacademy.insightonauth.service.UserEmailService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import com.nhnacademy.insightonauth.service.UserRoleService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController HTTP 계약 고정 테스트 — URL, 상태코드, 응답 형태.
 * 컨트롤러 분리·후속 리팩터링 시 회귀를 잡기 위한 것.
 */
@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@Import({GlobalExceptionHandler.class, LoginResponder.class})
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockitoBean UserAuthenticationService userAuthenticationService;
    @MockitoBean UserEmailService userEmailService;
    @MockitoBean UserManagementService userManagementService;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean UserRoleService userRoleService; // HeaderAuthenticationFilter 의존성

    @Test
    @DisplayName("POST /email/verify-request — 204")
    void emailVerifyRequest() throws Exception {
        mvc.perform(post("/api/v1/auth/email/verify-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "user@test.com" }
                                """))
                .andExpect(status().isNoContent());

        verify(userEmailService).emailVerifyRequest("user@test.com");
    }

    @Test
    @DisplayName("POST /email/verify-request — 이메일 형식 오류 400")
    void emailVerifyRequest_invalidEmail() throws Exception {
        mvc.perform(post("/api/v1/auth/email/verify-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "not-an-email" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /email/verify-confirm — 200, verificationToken 반환")
    void emailVerifyConfirm() throws Exception {
        when(userEmailService.emailVerifyConfirm("user@test.com", "123456")).thenReturn("vtoken");

        mvc.perform(post("/api/v1/auth/email/verify-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "user@test.com", "code": "123456" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationToken").value("vtoken"));
    }

    @Test
    @DisplayName("POST /check-email — 200, available 반환")
    void checkEmail() throws Exception {
        when(userManagementService.checkEmailAvailable("user@test.com")).thenReturn(true);

        mvc.perform(post("/api/v1/auth/check-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "user@test.com" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("POST /signup — 201, 가입 정보 반환")
    void signup() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        when(userManagementService.createUser(eq("user@test.com"), any(), eq("홍길동"),
                eq("01012345678"), eq(Role.MEMBER), eq("vtoken")))
                .thenReturn(new UserSignupResponse("user@test.com", "홍길동", "01012345678", now));

        mvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@test.com",
                                  "password": "Abcd1234!",
                                  "userName": "홍길동",
                                  "phoneNumber": "01012345678",
                                  "token": "vtoken"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.userName").value("홍길동"));
    }

    @Test
    @DisplayName("POST /signup — 필수값 누락 400")
    void signup_validation() throws Exception {
        mvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "user@test.com" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /login — 200, status=SUCCESS + accessToken, refreshToken 쿠키")
    void login() throws Exception {
        when(userAuthenticationService.login("user@test.com", "Abcd1234!"))
                .thenReturn(UserLoginResult.success("access-xyz", "refresh-abc"));
        when(jwtProvider.hasAdminRole("access-xyz")).thenReturn(false);

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "user@test.com", "password": "Abcd1234!" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.accessToken").value("access-xyz"))
                .andExpect(cookie().value("refreshToken", "refresh-abc"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Lax")));
    }

    @Test
    @DisplayName("POST /login — 탈퇴 복구 가능 계정은 200 PENDING_RESTORE + restoreToken (500 아님)")
    void login_pendingRestore() throws Exception {
        when(userAuthenticationService.login("gone@test.com", "Abcd1234!"))
                .thenReturn(UserLoginResult.pendingRestore("restore-tok"));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "gone@test.com", "password": "Abcd1234!" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_RESTORE"))
                .andExpect(jsonPath("$.restoreToken").value("restore-tok"))
                .andExpect(jsonPath("$.accessToken").isEmpty())
                .andExpect(header().doesNotExist("Set-Cookie"));

        verifyNoInteractions(jwtProvider);   // 토큰이 없으므로 admin 체크를 하지 않는다
    }

    @Test
    @DisplayName("POST /login — 관리자 계정은 일반 로그인 불가 401")
    void login_adminRejected() throws Exception {
        when(userAuthenticationService.login(any(), any()))
                .thenReturn(UserLoginResult.success("admin-token", "r"));
        when(jwtProvider.hasAdminRole("admin-token")).thenReturn(true);

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "admin@test.com", "password": "Abcd1234!" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /logout — 204, 헤더의 userId/토큰 전달")
    void logout() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")
                        .header("X-User-Id", "1")
                        .header("Authorization", "Bearer acc-token"))
                .andExpect(status().isNoContent());

        verify(userAuthenticationService).logout(1L, "acc-token");
    }

    @Test
    @DisplayName("POST /oauth/{provider} — 200, accessToken 은 바디 / refreshToken 은 쿠키")
    void oauthLogin() throws Exception {
        when(userAuthenticationService.oauthLogin("google", "code-123"))
                .thenReturn(UserLoginResult.success("acc", "ref"));

        mvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "code-123" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.accessToken").value("acc"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().value("refreshToken", "ref"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    @DisplayName("POST /oauth/{provider} — 탈퇴 복구 가능 계정은 200 PENDING_RESTORE + restoreToken")
    void oauthLogin_pendingRestore() throws Exception {
        when(userAuthenticationService.oauthLogin("google", "code-123"))
                .thenReturn(UserLoginResult.pendingRestore("restore-tok"));

        mvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "code-123" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_RESTORE"))
                .andExpect(jsonPath("$.restoreToken").value("restore-tok"))
                .andExpect(jsonPath("$.accessToken").isEmpty())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    @DisplayName("POST /refresh — 200, 새 accessToken 반환")
    void refresh() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtProvider.parse("good-refresh")).thenReturn(claims);
        when(userAuthenticationService.refresh(1L, "good-refresh"))
                .thenReturn(new TokenRefreshResponse("new-access"));

        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "good-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /refresh — 서명/만료 오류 시 401")
    void refresh_invalidToken() throws Exception {
        when(jwtProvider.parse("bad")).thenThrow(new JwtException("invalid"));

        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "bad")))
                .andExpect(status().isUnauthorized());
    }
}
