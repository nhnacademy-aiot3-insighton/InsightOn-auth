package com.nhnacademy.insightonauth.controller;

import com.nhnacademy.insightonauth.dto.mypage.MyInfoResponse;
import com.nhnacademy.insightonauth.dto.mypage.PasswordChangeRequest;
import com.nhnacademy.insightonauth.dto.mypage.RoleResponse;
import com.nhnacademy.insightonauth.dto.mypage.MyInfoUpdateRequest;
import com.nhnacademy.insightonauth.dto.oauth.OauthLoginRequest;
import com.nhnacademy.insightonauth.dto.oauth.OauthResponse;
import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.service.MyPageService;
import com.nhnacademy.insightonauth.service.OauthService;
import com.nhnacademy.insightonauth.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class MypageController {

    private static final String X_USER_ID = "X-User-Id";

    private final OauthService oauthService;
    private final MyPageService myPageService;
    private final UserManagementService userManagementService;

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<MyInfoResponse> findMyInfo(
            @RequestHeader(name = X_USER_ID) @Valid Long userId) {

        MyInfoResponse response = myPageService.findMyInfo(userId);
        return ResponseEntity.ok(response);
    }

    // 내 정보 수정
    @PutMapping("/me")
    public ResponseEntity<Void> updateMyInfo(
            @RequestHeader(name = X_USER_ID) @Valid Long userId,
            @RequestBody @Valid MyInfoUpdateRequest request) {

        userManagementService.updateUserName(userId, request.name());
        userManagementService.updatePhoneNumber(userId, request.phoneNumber());
        return ResponseEntity.ok().build();
    }

    // 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @RequestHeader(name = X_USER_ID) @Valid Long userId,
            @RequestHeader("Authorization") String token) {
        String accessToken = token.replace("Bearer ", "");
        userManagementService.withdraw(userId, accessToken);
        return ResponseEntity.noContent().build();
    }

    // 비밀번호 변경
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader(name = X_USER_ID) @Valid Long userId,
            @RequestBody @Valid PasswordChangeRequest request) {

        myPageService.updatePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    // 내 권한 목록 조회
    @GetMapping("/me/roles")
    public ResponseEntity<List<RoleResponse>> findMyRoles(
            @RequestHeader(name = X_USER_ID) @Valid Long userId) {

        List<RoleResponse> response = myPageService.findMyRoles(userId);
        return ResponseEntity.ok(response);
    }

    // 연동 소셜 계정 목록
    @GetMapping("/me/oauths")
    public ResponseEntity<List<OauthResponse>> findMyOauths(
            @RequestHeader(name = X_USER_ID) @Valid Long userId) {

        List<OauthResponse> response = myPageService.findMyOauths(userId);
        return ResponseEntity.ok(response);
    }

    // 소셜 계정 신규 연동
    @PostMapping("/me/oauths/{provider}")
    public ResponseEntity<Void> linkOauth(
            @RequestHeader(name = X_USER_ID) @Valid Long userId,
            @PathVariable String provider,
            @RequestBody @Valid OauthLoginRequest request) {

        myPageService.linkOauth(userId, provider, request.code());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 소셜 계정 연동 해제
    @DeleteMapping("/me/oauths/{oauthId}")
    public ResponseEntity<Void> unlinkOauth(
            @RequestHeader(name = X_USER_ID) @Valid Long userId,
            @PathVariable Long oauthId) {

        User user = userManagementService.findById(userId);
        oauthService.delete(user, oauthId);
        return ResponseEntity.noContent().build();
    }
}
