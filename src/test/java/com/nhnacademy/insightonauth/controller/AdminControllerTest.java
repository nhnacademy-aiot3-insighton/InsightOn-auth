package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.dto.admin.AdminFindUsersResponse;
import com.nhnacademy.insightonauth.dto.admin.AdminUserDetailResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.entity.Role;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.handler.GlobalExceptionHandler;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.service.AdminUserService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminController HTTP 계약 고정 테스트.
 */
@WebMvcTest(controllers = AdminController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class AdminControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean AdminUserService adminUserService;
    @MockitoBean UserAuthenticationService userAuthenticationService;
    @MockitoBean UserManagementService userManagementService;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean UserRoleService userRoleService; // HeaderAuthenticationFilter 의존성

    @Test
    @DisplayName("POST /login — 관리자면 200, refreshToken 쿠키")
    void adminLogin() throws Exception {
        when(userAuthenticationService.login("admin@test.com", "Abcd1234!"))
                .thenReturn(UserLoginResponse.success("admin-acc", "admin-ref"));
        when(jwtProvider.hasAdminRole("admin-acc")).thenReturn(true);

        mvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "admin@test.com", "password": "Abcd1234!" }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().value("refreshToken", "admin-ref"));
    }

    @Test
    @DisplayName("POST /login — 탈퇴 복구 가능 관리자 계정은 200 PENDING_RESTORE (500 아님)")
    void adminLogin_pendingRestore() throws Exception {
        when(userAuthenticationService.login("admin@test.com", "Abcd1234!"))
                .thenReturn(UserLoginResponse.pendingRestore("restore-tok"));

        mvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "admin@test.com", "password": "Abcd1234!" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_RESTORE"))
                .andExpect(jsonPath("$.restoreToken").value("restore-tok"))
                .andExpect(jsonPath("$.accessToken").isEmpty());

        verifyNoInteractions(jwtProvider);
    }

    @Test
    @DisplayName("POST /login — 일반 회원은 관리자 로그인 불가 401")
    void adminLogin_nonAdminRejected() throws Exception {
        when(userAuthenticationService.login(any(), any()))
                .thenReturn(UserLoginResponse.success("member-acc", "r"));
        when(jwtProvider.hasAdminRole("member-acc")).thenReturn(false);

        mvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "member@test.com", "password": "Abcd1234!" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users — 200, 페이지 결과")
    void findUsers() throws Exception {
        Page<AdminFindUsersResponse> page = new PageImpl<>(List.of(
                new AdminFindUsersResponse(1L, "a@test.com", "유저A", Status.ACTIVE,
                        OffsetDateTime.now(ZoneOffset.UTC))));
        when(adminUserService.findUsers(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("a@test.com"));
    }

    @Test
    @DisplayName("GET /users/{userId} — 200, 상세")
    void findUserDetail() throws Exception {
        when(adminUserService.findUserDetail(1L)).thenReturn(
                new AdminUserDetailResponse(1L, "a@test.com", "유저A", Status.ACTIVE, List.of(Role.MEMBER)));

        mvc.perform(get("/api/v1/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("유저A"))
                .andExpect(jsonPath("$.roles[0]").value("MEMBER"));
    }

    @Test
    @DisplayName("POST /users/{userId}/block — 204")
    void block() throws Exception {
        mvc.perform(post("/api/v1/admin/users/1/block"))
                .andExpect(status().isNoContent());
        verify(adminUserService).block(1L);
    }

    @Test
    @DisplayName("POST /users/{userId}/sleep — 204")
    void sleep() throws Exception {
        mvc.perform(post("/api/v1/admin/users/1/sleep"))
                .andExpect(status().isNoContent());
        verify(adminUserService).sleep(1L);
    }

    @Test
    @DisplayName("POST /users/{userId}/activate — 204")
    void activate() throws Exception {
        mvc.perform(post("/api/v1/admin/users/1/activate"))
                .andExpect(status().isNoContent());
        verify(adminUserService).activate(1L);
    }

    @Test
    @DisplayName("PUT /users/{userId}/roles — 200")
    void changeRole() throws Exception {
        mvc.perform(put("/api/v1/admin/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "role": "ADMIN" }
                                """))
                .andExpect(status().isOk());
        verify(adminUserService).addUserRole(1L, Role.ADMIN);
    }

    @Test
    @DisplayName("PUT /users/{userId}/roles — role 누락 400")
    void changeRole_validation() throws Exception {
        mvc.perform(put("/api/v1/admin/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/{userId}/force-logout — 204")
    void forceLogout() throws Exception {
        mvc.perform(post("/api/v1/admin/users/1/force-logout"))
                .andExpect(status().isNoContent());
        verify(adminUserService).forceLogout(1L);
    }
}
