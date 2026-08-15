package com.nhnacademy.insightonauth.controller;


import com.nhnacademy.insightonauth.dto.admin.AdminFindUsersResponse;
import com.nhnacademy.insightonauth.dto.admin.AdminUserDetailResponse;
import com.nhnacademy.insightonauth.dto.admin.RoleChangeRequest;
import com.nhnacademy.insightonauth.dto.admin.StatusChangeRequest;
import com.nhnacademy.insightonauth.entity.Status;
import com.nhnacademy.insightonauth.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;

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
