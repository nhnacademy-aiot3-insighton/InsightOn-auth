package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.handler.GlobalExceptionHandler;
import com.nhnacademy.insightonauth.service.TokenBlacklistService;
import com.nhnacademy.insightonauth.service.UserAuthenticationService;
import com.nhnacademy.insightonauth.service.UserRoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TokenController HTTP 계약 고정 테스트 — 게이트웨이용 블랙리스트 조회.
 */
@WebMvcTest(controllers = TokenController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class TokenControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserAuthenticationService userAuthenticationService; // HeaderAuthenticationFilter 의존성
    @MockitoBean UserRoleService userRoleService;                     // HeaderAuthenticationFilter 의존성

    @Test
    @DisplayName("GET /tokens/{jti}/blacklisted — 200, true")
    void blacklisted() throws Exception {
        when(tokenBlacklistService.isBlacklisted("jti-abc")).thenReturn(true);

        mvc.perform(get("/api/v1/auth/tokens/{jti}/blacklisted", "jti-abc"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("GET /tokens/{jti}/blacklisted — 200, false")
    void notBlacklisted() throws Exception {
        when(tokenBlacklistService.isBlacklisted("jti-xyz")).thenReturn(false);

        mvc.perform(get("/api/v1/auth/tokens/{jti}/blacklisted", "jti-xyz"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
