package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.dto.mypage.MyInfoResponse;
import com.nhnacademy.insightonauth.dto.mypage.RoleResponse;
import com.nhnacademy.insightonauth.dto.oauth.OauthResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.handler.GlobalExceptionHandler;
import com.nhnacademy.insightonauth.service.MyPageService;
import com.nhnacademy.insightonauth.service.OauthService;
import com.nhnacademy.insightonauth.service.UserAuthenticationService;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MypageController HTTP 계약 고정 테스트. 모든 엔드포인트가 X-User-Id 헤더 필요.
 */
@WebMvcTest(controllers = MypageController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class MypageControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean OauthService oauthService;
    @MockitoBean MyPageService myPageService;
    @MockitoBean UserManagementService userManagementService;
    @MockitoBean UserAuthenticationService userAuthenticationService; // HeaderAuthenticationFilter 의존성
    @MockitoBean UserRoleService userRoleService;                     // HeaderAuthenticationFilter 의존성

    @Test
    @DisplayName("GET /me — 200, 내 정보")
    void findMyInfo() throws Exception {
        when(myPageService.findMyInfo(1L)).thenReturn(
                new MyInfoResponse("me@test.com", "나", "01012345678",
                        OffsetDateTime.now(ZoneOffset.UTC), "그룹A"));

        mvc.perform(get("/api/v1/users/me").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@test.com"))
                .andExpect(jsonPath("$.groupName").value("그룹A"));
    }

    @Test
    @DisplayName("GET /me — X-User-Id 헤더 누락 시 현재 500 (알려진 이슈: GlobalExceptionHandler catch-all이 4xx 프레임워크 예외를 삼킴)")
    void findMyInfo_missingHeader() throws Exception {
        // 정상적으로는 MissingRequestHeaderException → 400 이어야 하나,
        // GlobalExceptionHandler 의 @ExceptionHandler(Exception.class) 가 먼저 잡아 500 반환.
        // 로그인 500 버그와 동일한 근본 원인. 계약 테스트로 현재 동작을 고정해 둠.
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("PUT /me — 200, 이름/전화번호 수정")
    void updateMyInfo() throws Exception {
        mvc.perform(put("/api/v1/users/me")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "새이름", "phoneNumber": "01088887777" }
                                """))
                .andExpect(status().isOk());

        verify(userManagementService).updateUserName(1L, "새이름");
        verify(userManagementService).updatePhoneNumber(1L, "01088887777");
    }

    @Test
    @DisplayName("PUT /me — 전화번호 형식 오류 400")
    void updateMyInfo_validation() throws Exception {
        mvc.perform(put("/api/v1/users/me")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "새이름", "phoneNumber": "123" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /me — 204, Bearer 토큰 파싱해 탈퇴 호출")
    void withdraw() throws Exception {
        mvc.perform(delete("/api/v1/users/me")
                        .header("X-User-Id", "1")
                        .header("Authorization", "Bearer acc-token"))
                .andExpect(status().isNoContent());

        verify(userManagementService).withdraw(1L, "acc-token");
    }

    @Test
    @DisplayName("PUT /me/password — 200")
    void changePassword() throws Exception {
        mvc.perform(put("/api/v1/users/me/password")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "currentPassword": "Old1234!", "newPassword": "New1234!" }
                                """))
                .andExpect(status().isOk());

        verify(myPageService).updatePassword(1L, "Old1234!", "New1234!");
    }

    @Test
    @DisplayName("GET /me/roles — 200, 권한 목록")
    void findMyRoles() throws Exception {
        when(myPageService.findMyRoles(1L)).thenReturn(List.of(new RoleResponse(Role.MEMBER)));

        mvc.perform(get("/api/v1/users/me/roles").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("MEMBER"));
    }

    @Test
    @DisplayName("GET /me/oauths — 200, 연동 목록")
    void findMyOauths() throws Exception {
        when(myPageService.findMyOauths(1L)).thenReturn(List.of(new OauthResponse(10L, "google")));

        mvc.perform(get("/api/v1/users/me/oauths").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("google"));
    }

    // 소셜 계정 신규 연동은 브라우저 주도 왕복이라 auth 의 GET /oauth/link/authorize/{provider} 로 이동함
    // (AuthControllerTest 에서 콜백 분기를 검증)

    @Test
    @DisplayName("DELETE /me/oauths/{oauthId} — 204")
    void unlinkOauth() throws Exception {
        User user = new User("me@test.com", "나", "01012345678");
        when(userManagementService.findById(1L)).thenReturn(user);

        mvc.perform(delete("/api/v1/users/me/oauths/10").header("X-User-Id", "1"))
                .andExpect(status().isNoContent());

        verify(oauthService).delete(user, 10L);
    }
}
