package com.nhnacademy.insightonauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.insightonauth.controller.support.LoginResponder;
import com.nhnacademy.insightonauth.controller.support.OauthWebSupport;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResult;
import com.nhnacademy.insightonauth.dto.auth.UserSignupResponse;
import com.nhnacademy.insightonauth.dto.auth.TokenRefreshResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.exception.oauth.OauthAlreadyLinkedException;
import com.nhnacademy.insightonauth.exception.oauth.OauthLinkedToOtherAccountException;
import com.nhnacademy.insightonauth.exception.signup.EmailAlreadyRegisteredException;
import com.nhnacademy.insightonauth.exception.user.ManagerGroupExistsException;
import com.nhnacademy.insightonauth.handler.GlobalExceptionHandler;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.service.MyPageService;
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
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @MockitoBean
    OauthWebSupport oauthWebSupport; // 브라우저 주도 OAuth 엔드포인트 의존성
    @MockitoBean MyPageService myPageService; // /oauth/callback 연동(link) 분기 의존성

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
    @DisplayName("POST /login — 탈퇴 복구 가능 계정은 200 PENDING_RESTORE (500 아님)")
    void login_pendingRestore() throws Exception {
        when(userAuthenticationService.login("gone@test.com", "Abcd1234!"))
                .thenReturn(UserLoginResult.pendingRestore());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "gone@test.com", "password": "Abcd1234!" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_RESTORE"))
                .andExpect(jsonPath("$.restoreToken").doesNotExist())
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
    @DisplayName("POST /oauth/{provider} — 탈퇴 복구 가능 계정은 200 PENDING_RESTORE")
    void oauthLogin_pendingRestore() throws Exception {
        when(userAuthenticationService.oauthLogin("google", "code-123"))
                .thenReturn(UserLoginResult.pendingRestore());

        mvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "code-123" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_RESTORE"))
                .andExpect(jsonPath("$.restoreToken").doesNotExist())
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

    // ---------- 브라우저 주도 OAuth ----------

    private void stubFront() {
        when(oauthWebSupport.front(anyString()))
                .thenAnswer(inv -> "https://front.test" + inv.<String>getArgument(0));
        when(oauthWebSupport.expiredStateCookie())
                .thenReturn(ResponseCookie.from(OauthWebSupport.STATE_COOKIE, "").maxAge(0).build());
    }

    @Test
    @DisplayName("GET /oauth/authorize/{provider} — 지원하는 provider면 302 + state 쿠키")
    void oauthAuthorize() throws Exception {
        when(oauthWebSupport.supports("google")).thenReturn(true);
        when(oauthWebSupport.stateCookie(anyString()))
                .thenReturn(ResponseCookie.from(OauthWebSupport.STATE_COOKIE, "nonce.google").build());
        when(oauthWebSupport.authorizeUrl(eq("google"), anyString()))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?state=nonce.google");

        mvc.perform(get("/api/v1/auth/oauth/authorize/google"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://accounts.google.com/o/oauth2/v2/auth?state=nonce.google"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("GET /oauth/authorize/{provider} — 지원 안 하는 provider면 로그인 오류로 302")
    void oauthAuthorize_unsupported() throws Exception {
        stubFront();
        when(oauthWebSupport.supports("kakao")).thenReturn(false);

        mvc.perform(get("/api/v1/auth/oauth/authorize/kakao"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/login?oauthError=1"));
    }

    @Test
    @DisplayName("GET /oauth/link/authorize/{provider} — 정상 로그인 상태면 302 + state 쿠키")
    void oauthLinkAuthorize() throws Exception {
        when(oauthWebSupport.supports("google")).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(jwtProvider.parse("valid-access")).thenReturn(claims);
        when(oauthWebSupport.stateCookie(anyString()))
                .thenReturn(ResponseCookie.from(OauthWebSupport.STATE_COOKIE, "nonce.google.link.5").build());
        when(oauthWebSupport.authorizeUrl(eq("google"), anyString()))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?state=nonce.google.link.5");

        mvc.perform(get("/api/v1/auth/oauth/link/authorize/google")
                        .cookie(new Cookie("accessToken", "valid-access")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://accounts.google.com/o/oauth2/v2/auth?state=nonce.google.link.5"));
    }

    @Test
    @DisplayName("GET /oauth/link/authorize/{provider} — accessToken 쿠키 없으면 마이페이지 오류로 302")
    void oauthLinkAuthorize_noAccessToken() throws Exception {
        stubFront();
        when(oauthWebSupport.supports("google")).thenReturn(true);

        mvc.perform(get("/api/v1/auth/oauth/link/authorize/google"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/mypage?linkError=1"));
    }

    @Test
    @DisplayName("GET /oauth/link/authorize/{provider} — 지원 안 하는 provider면 마이페이지 오류로 302")
    void oauthLinkAuthorize_unsupported() throws Exception {
        stubFront();
        when(oauthWebSupport.supports("kakao")).thenReturn(false);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(jwtProvider.parse("valid-access")).thenReturn(claims);

        mvc.perform(get("/api/v1/auth/oauth/link/authorize/kakao")
                        .cookie(new Cookie("accessToken", "valid-access")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/mypage?linkError=1"));
    }

    // ---------- GET /oauth/callback ----------

    @Test
    @DisplayName("callback — provider 오류(error 파라미터)면 로그인 오류로 302")
    void oauthCallback_providerError() throws Exception {
        stubFront();

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("error", "access_denied")
                        .param("code", "c")
                        .param("state", "nonce.google")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "nonce.google")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/login?oauthError=1"));
    }

    @Test
    @DisplayName("callback — state 쿠키가 없으면 로그인 오류로 302")
    void oauthCallback_missingExpectedState() throws Exception {
        stubFront();

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/login?oauthError=1"));
    }

    @Test
    @DisplayName("callback — state 불일치면 연동 왕복이었으면 마이페이지 오류로 302")
    void oauthCallback_stateMismatch_link() throws Exception {
        stubFront();

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google.link.5")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "different.google.link.5")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/mypage?linkError=1"));
    }

    @Test
    @DisplayName("callback — 연동 중 계정이 바뀌면(state userId ≠ 현재 로그인 유저) 마이페이지 인증오류로 302")
    void oauthCallback_link_userMismatch() throws Exception {
        stubFront();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("999");
        when(jwtProvider.parse("current-access")).thenReturn(claims);

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google.link.5")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "nonce.google.link.5"))
                        .cookie(new Cookie("accessToken", "current-access")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/mypage?linkError=auth"));
    }

    @Test
    @DisplayName("callback — 연동 성공이면 마이페이지 linked=1 로 302")
    void oauthCallback_link_success() throws Exception {
        stubFront();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(jwtProvider.parse("current-access")).thenReturn(claims);

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google.link.5")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "nonce.google.link.5"))
                        .cookie(new Cookie("accessToken", "current-access")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/mypage?linked=1"));

        verify(myPageService).linkOauth(5L, "google", "c");
    }

    @Test
    @DisplayName("callback — 병합 확인 왕복이면 confirmMerge 호출 후 mypage?merged=1 로 302")
    void oauthCallback_link_mergeSuccess() throws Exception {
        stubFront();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(jwtProvider.parse("current-access")).thenReturn(claims);

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google.link.5.merge.9")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "nonce.google.link.5.merge.9"))
                        .cookie(new Cookie("accessToken", "current-access")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/mypage?merged=1"));

        verify(myPageService).confirmMerge(5L, 9L, "google", "c");
        verify(myPageService, never()).linkOauth(any(), any(), any());
    }

    @Test
    @DisplayName("callback — 이미 연동된 계정이면 mypage?linkError=already 로 302")
    void oauthCallback_link_alreadyLinked() throws Exception {
        stubFront();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(jwtProvider.parse("current-access")).thenReturn(claims);
        doThrow(new OauthAlreadyLinkedException("이미 연동됨"))
                .when(myPageService).linkOauth(5L, "google", "c");

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google.link.5")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "nonce.google.link.5"))
                        .cookie(new Cookie("accessToken", "current-access")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/mypage?linkError=already"));
    }

    @Test
    @DisplayName("callback — 다른 계정에 이미 연동돼 있으면 conflictUserId 포함 302")
    void oauthCallback_link_linkedToOtherAccount() throws Exception {
        stubFront();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(jwtProvider.parse("current-access")).thenReturn(claims);
        doThrow(new OauthLinkedToOtherAccountException("다른 계정에 연동됨", 42L))
                .when(myPageService).linkOauth(5L, "google", "c");

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google.link.5")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "nonce.google.link.5"))
                        .cookie(new Cookie("accessToken", "current-access")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "https://front.test/mypage?linkError=other_account&conflictUserId=42&provider=google"));
    }

    @Test
    @DisplayName("callback — 그룹 관리자 계정이면 mypage?linkError=manager_account 로 302")
    void oauthCallback_link_managerGroupExists() throws Exception {
        stubFront();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(jwtProvider.parse("current-access")).thenReturn(claims);
        doThrow(new ManagerGroupExistsException("그룹 관리자"))
                .when(myPageService).linkOauth(5L, "google", "c");

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google.link.5")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "nonce.google.link.5"))
                        .cookie(new Cookie("accessToken", "current-access")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/mypage?linkError=manager_account"));
    }

    @Test
    @DisplayName("callback — 연동 중 알 수 없는 오류면 mypage?linkError=1 로 302")
    void oauthCallback_link_unknownError() throws Exception {
        stubFront();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(jwtProvider.parse("current-access")).thenReturn(claims);
        doThrow(new RuntimeException("boom"))
                .when(myPageService).linkOauth(5L, "google", "c");

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google.link.5")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "nonce.google.link.5"))
                        .cookie(new Cookie("accessToken", "current-access")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/mypage?linkError=1"));
    }

    @Test
    @DisplayName("callback — 로그인 성공이면 프론트 /oauth/complete 로 302 + 토큰 쿠키")
    void oauthCallback_login_success() throws Exception {
        stubFront();
        when(userAuthenticationService.oauthLogin("google", "c"))
                .thenReturn(UserLoginResult.success("acc", "ref"));

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "nonce.google")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/oauth/complete"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("callback — 로그인 시 탈퇴 복구 대기 계정이면 /reactivate 로 302")
    void oauthCallback_login_pendingRestore() throws Exception {
        stubFront();
        when(userAuthenticationService.oauthLogin("google", "c"))
                .thenReturn(UserLoginResult.pendingRestore());

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "nonce.google")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/reactivate"));
    }

    @Test
    @DisplayName("callback — 로그인 시 이미 가입된 이메일이면 email_taken 오류로 302")
    void oauthCallback_login_emailAlreadyRegistered() throws Exception {
        stubFront();
        when(userAuthenticationService.oauthLogin("google", "c"))
                .thenThrow(new EmailAlreadyRegisteredException("이미 가입된 이메일"));

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "nonce.google")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/login?oauthError=email_taken"));
    }

    @Test
    @DisplayName("callback — 로그인 중 알 수 없는 오류면 오류로 302")
    void oauthCallback_login_unknownError() throws Exception {
        stubFront();
        when(userAuthenticationService.oauthLogin("google", "c"))
                .thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/v1/auth/oauth/callback")
                        .param("code", "c")
                        .param("state", "nonce.google")
                        .cookie(new Cookie(OauthWebSupport.STATE_COOKIE, "nonce.google")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://front.test/login?oauthError=1"));
    }
}
