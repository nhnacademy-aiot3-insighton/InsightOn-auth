package com.nhnacademy.insightonauth.controller;


import com.nhnacademy.insightonauth.dto.admin.AdminFindUsersResponse;
import com.nhnacademy.insightonauth.dto.admin.AdminUserDetailResponse;
import com.nhnacademy.insightonauth.dto.admin.RoleChangeRequest;
import com.nhnacademy.insightonauth.dto.auth.LoginResponse;
import com.nhnacademy.insightonauth.dto.auth.UserLoginRequest;
import com.nhnacademy.insightonauth.dto.auth.UserLoginResponse;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.exception.auth.InvalidCredentialsException;
import com.nhnacademy.insightonauth.provider.JwtProvider;
import com.nhnacademy.insightonauth.service.AdminUserService;
import com.nhnacademy.insightonauth.service.UserAuthenticationService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;
    private final UserAuthenticationService userAuthenticationService;
    private final UserManagementService userManagementService;
    private final JwtProvider jwtProvider;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> doLogin(
            @RequestBody @Valid UserLoginRequest userLoginRequest) {
        UserLoginResponse userLoginResponse = userAuthenticationService.login(userLoginRequest.email(), userLoginRequest.password());

        // 탈퇴 후 복구 가능 기간 내 관리자 계정 — 로그인 성공이 아니라 "복구 안내" 상태.
        // accessToken 이 없으므로 admin 체크(hasAdminRole) 대상이 아니다.
        if ("PENDING_RESTORE".equals(userLoginResponse.status())) {
            return ResponseEntity.ok(LoginResponse.pendingRestore(userLoginResponse.restoreToken()));
        }

        // 공통 검증기 사용 — 타입 안전, roles 없음/형식오류는 안전하게 false
        if (!jwtProvider.hasAdminRole(userLoginResponse.accessToken())) {
            // 계정 열거 방지: 관리자 아님 / 비번 오류 / 없는 계정 모두 동일 메시지
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 도커용
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", userLoginResponse.refreshToken())
                .httpOnly(true)
                .secure(true)           // https라 true
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(7))
                .build();               // domain 안 박음

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())   // refresh 쿠키 헤더에 설정
                .body(LoginResponse.success(userLoginResponse.accessToken()));

    }

    // 회원 목록 조회 (검색·페이징)
    @GetMapping("/users")
    public ResponseEntity<Page<AdminFindUsersResponse>> findUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Status status,
            Pageable pageable) {

        Page<AdminFindUsersResponse> response = adminUserService.findUsers(email, userName, status, pageable);
        return ResponseEntity.ok(response);
    }

    // 회원 상세 조회
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDetailResponse> findUserDetail(@PathVariable Long userId) {
        AdminUserDetailResponse response = adminUserService.findUserDetail(userId);
        return ResponseEntity.ok(response);
    }

    // 회원 계정 차단
    @PostMapping("/users/{userId}/block")
    public ResponseEntity<Void> block(@PathVariable Long userId) {
        adminUserService.block(userId);
        return ResponseEntity.noContent().build();
    }

    // 회원 계정 휴면 전환
    @PostMapping("/users/{userId}/sleep")
    public ResponseEntity<Void> sleep(@PathVariable Long userId) {
        adminUserService.sleep(userId);
        return ResponseEntity.noContent().build();
    }

    // 회원 계정 활성화 (복구)
    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long userId) {
        adminUserService.activate(userId);
        return ResponseEntity.noContent().build();
    }

    // 회원 권한 변경
    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<Void> changeRole(
            @PathVariable Long userId,
            @RequestBody @Valid RoleChangeRequest request) {

        adminUserService.addUserRole(userId, request.role());
        return ResponseEntity.ok().build();
    }

    // 강제 로그아웃
    @PostMapping("/users/{userId}/force-logout")
    public ResponseEntity<Void> forceLogout(
            @PathVariable Long userId) {
        adminUserService.forceLogout(userId);
        return ResponseEntity.noContent().build();
    }
}
