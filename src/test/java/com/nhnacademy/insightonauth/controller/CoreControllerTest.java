package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.exception.UserNotFoundException;
import com.nhnacademy.insightonauth.handler.GlobalExceptionHandler;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CoreController HTTP 계약 고정 테스트 — 내부 서비스 간 유저 조회 (/internal/v1/users).
 */
@WebMvcTest(controllers = CoreController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class CoreControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean UserManagementService userManagementService;
    @MockitoBean UserAuthenticationService userAuthenticationService; // HeaderAuthenticationFilter 의존성
    @MockitoBean UserRoleService userRoleService;                     // HeaderAuthenticationFilter 의존성

    private User user(long id) {
        User u = new User("u@test.com", "유저", "01012345678");
        ReflectionTestUtils.setField(u, "userId", id);
        return u;
    }

    @Test
    @DisplayName("GET /internal/v1/users/{id} — 200, AuthUserResponse")
    void getUserById() throws Exception {
        when(userManagementService.findById(1L)).thenReturn(user(1L));

        mvc.perform(get("/internal/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.userName").value("유저"))
                .andExpect(jsonPath("$.userStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /internal/v1/users/{id} — 없는 유저 404")
    void getUserById_notFound() throws Exception {
        when(userManagementService.findById(999L))
                .thenThrow(new UserNotFoundException("유저를 찾을 수 없습니다."));

        mvc.perform(get("/internal/v1/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /internal/v1/users/invite/{email} — 200")
    void getUserByEmail() throws Exception {
        when(userManagementService.findByEmail("u@test.com")).thenReturn(user(2L));

        mvc.perform(get("/internal/v1/users/invite/{email}", "u@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2));
    }
}
